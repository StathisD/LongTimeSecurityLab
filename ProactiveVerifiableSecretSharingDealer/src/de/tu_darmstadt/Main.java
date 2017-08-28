package de.tu_darmstadt;

import com.j256.ormlite.dao.CloseableIterator;
import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Database.*;
import static de.tu_darmstadt.Parameters.*;

public class Main {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            FILE_PATH = args[0];
            FILE_NAME = args[1];
            SHAREHOLDERS = Integer.parseInt(args[2]);
            NEEDED_SHARES = Integer.parseInt(args[3]);
            initiateDb();

            dbSemaphore.acquire();
            PedersenParameters params = pedersenParametersDao.queryForId("params");
            dbSemaphore.release();
            committer = new PedersenCommitter(params);
            MODULUS = params.getQ();
            pedersenParameters = params;

            int input;
            do {
                show("Please specify what you want to do:");
                show("(Type 1 for sharing a new file)");
                show("(Type 2 for decrypting a File)");
                show("(Type 3 to add a new ShareHolder)");
                input = scanner.nextInt();
            } while (input <= 0 || input > 3);

            Timestamp start = new Timestamp(System.currentTimeMillis());

            switch (input) {
                case 1:
                    encryptFile(input);
                    break;
                case 2:
                    decryptFile(input);
                    break;
                case 3:
                    addShareHolder();
                    break;
            }

            Timestamp end = new Timestamp(System.currentTimeMillis());
            show(end.getTime() - start.getTime());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void encryptFile(int mode) {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILE_PATH, "r");
            sourceFile.seek(0L);
            long targetFileSize = sourceFile.length();
            boolean verifiability = false;

            initializeParameters(targetFileSize, 0, verifiability);

            StoredFile storedFile = new StoredFile(FILE_NAME, FILE_PATH, MODULUS, SHAREHOLDERS, NEEDED_SHARES, targetFileSize);

            dbSemaphore.acquire();

            CloseableIterator<ShareHolder> iterator = shareholdersDao.closeableIterator();
            try {
                int j = 0;
                shareHolders = new ShareHolder[SHAREHOLDERS];
                while (iterator.hasNext() && j < SHAREHOLDERS) {
                    ShareHolder shareHolder = iterator.next();
                    shareHolder.setxValue(BigInteger.valueOf(j + 1));
                    shareHolders[j] = shareHolder;
                    j++;
                }
            } finally {
                // close it at the end to close underlying SQL statement
                iterator.close();
            }
            dbSemaphore.release();


            ExecutorService socketPool = SSLClient.openNewConnections(mode);

            for (int i = 0; i<SHAREHOLDERS; i++){
                sslClients[i].xValue = i + 1;
            }

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            long encrypted = 0;
            long processed = 0;
            byte buffer[];
            Future[] futures = new Future[THREADS];
            int numberOfThreads = 0;
            boolean lastBuffer = false;

            while (processed < targetFileSize) {

                for (int i = 0; i < THREADS; i++) {

                    if (targetFileSize - processed >= BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                    } else {
                        buffer = new byte[(int) (targetFileSize - processed)];
                        lastBuffer = true;
                    }
                    sourceFile.readFully(buffer);
                    EncryptionTask task = new EncryptionTask(buffer);
                    futures[i] = pool.submit(task);
                    processed += buffer.length;
                    numberOfThreads = i;

                    if (lastBuffer) {
                        break;
                    }
                }

                for (int i = 0; i <= numberOfThreads; i++) {
                    BigInteger[][] taskBuffer = (BigInteger[][]) futures[i].get();
                    encrypted += taskBuffer[0].length;

                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        sslClients[j].numberQueue.put(taskBuffer[j]);
                    }
                }
            }
            int numbersInFile = (int) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCK_SIZE);
            if (VERIFIABILITY) numbersInFile = numbersInFile * (NEEDED_SHARES + 2);

            pool.shutdown();
            socketPool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);
            socketPool.awaitTermination(10, TimeUnit.MINUTES);

            boolean success = true;
            for (int i = 0; i < SHAREHOLDERS; i++) {
                if (!sslClients[i].status.equals("successful")) {
                    success = false;
                }
            }
            if (encrypted == numbersInFile && success) {
                show("File Storing completed successfully");
                new File(FILE_PATH).delete();
                dbSemaphore.acquire();
                storedFileDao.createIfNotExists(storedFile);
                for (ShareHolder shareHolder : shareHolders) {
                    ManyToMany manyToMany = new ManyToMany(shareHolder, storedFile, shareHolder.getxValue());
                    manyToManyDao.createIfNotExists(manyToMany);
                }
                dbSemaphore.release();
            } else {
                show("ERROR in File Storing");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void decryptFile(int mode) {
        try {
            dbSemaphore.acquire();
            StoredFile storedFile = storedFileDao.queryForId(FILE_NAME);
            ShareHolder[] shareHolders = new ShareHolder[SHAREHOLDERS];
            shareHolders = lookupShareHoldersForStoredFile(storedFile).toArray(shareHolders);
            dbSemaphore.release();

            NEEDED_SHARES = storedFile.getNeededShares();
            SHAREHOLDERS = storedFile.getNumberOfShareholders();

            initializeParameters(storedFile.getFileSize(), 1, false);

            ExecutorService socketPool = SSLClient.openNewConnections(mode);

            BigInteger[] xValues = new BigInteger[NEEDED_SHARES];
            sslClients = new SSLClient[SHAREHOLDERS];

            for (int i = 0; i < SHAREHOLDERS; i++) {
                SSLClient sslClient;
                if (i < NEEDED_SHARES) {
                    sslClient = new SSLClient(shareHolders[i], 2);
                    xValues[i] = lookupXvalueForShareHolderAndShare(shareHolders[i], storedFile);
                    sslClient.xValue = xValues[i].intValue();
                    sslClients[i] = sslClient;
                } else {
                    sslClient = new SSLClient(shareHolders[i], 3);
                    sslClients[i] = sslClient;
                }
                socketPool.submit(sslClient);
            }


            //compute Lagrange Coefficients
            BigIntegerPolynomial.computeLagrangeCoefficients(xValues, MODULUS);

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long destStartingByte = 0;
            long processed = 0;
            int numberOfThreads = 0;

            Future[] futures = new Future[THREADS];

            while (processed < SHARES_FILE_SIZE) {

                for (int i = 0; i < THREADS; i++) {
                    byte buffer[][] = new byte[NEEDED_SHARES][];
                    for (int j = 0; j < NEEDED_SHARES; j++) {
                        buffer[j] = sslClients[j].byteQueue.poll(10, TimeUnit.MINUTES);
                    }
                    int numbers = buffer[0].length / SHARE_SIZE;
                    DecryptionTask task = new DecryptionTask(buffer, destStartingByte);
                    futures[i] = pool.submit(task);
                    destStartingByte += numbers*BLOCK_SIZE;
                    processed += buffer[0].length;
                    numberOfThreads = i;
                    if (processed >= SHARES_FILE_SIZE) break;
                }

                for (int i = 0; i <= numberOfThreads; i++) {
                    int returnValue = (int) futures[i].get();
                    if (returnValue != 0) show("Error");
                }
            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);

            socketPool.shutdown();
            socketPool.awaitTermination(10, TimeUnit.MINUTES);

            dbSemaphore.acquire();
            storedFileDao.deleteById(storedFile.getName());
            CloseableIterator<ManyToMany> iterator = manyToManyDao.closeableIterator();
            try {
                while (iterator.hasNext()) {
                    ManyToMany manyToMany = iterator.next();
                    if (manyToMany.storedFile.getName().equals(storedFile.getName())) {
                        manyToManyDao.delete(manyToMany);
                    }
                }
            } finally {
                // close it at the end to close underlying SQL statement
                iterator.close();
            }
            dbSemaphore.release();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static void addShareHolder(){
        try{
            dbSemaphore.acquire();
            ShareHolder shareHolder = new ShareHolder("Server" + 0, "localhost", 8000);
            shareholdersDao.createIfNotExists(shareHolder);

            dbSemaphore.release();

        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
