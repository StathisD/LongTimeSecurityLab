package de.tu_darmstadt;

import com.j256.ormlite.dao.CloseableIterator;
import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;
import static de.tu_darmstadt.Database.*;


public class Main {
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {

        FILE_PATH = args[0];
        SHAREHOLDERS = Integer.parseInt(args[1]);
        NEEDED_SHARES = Integer.parseInt(args[2]);
        initiateDb();

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
    }

    private static void encryptFile(int mode) {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILE_PATH, "r");
            sourceFile.seek(0L);
            long targetFileSize = sourceFile.length();
            int bits = 1024;
            boolean verifiability = false;

            initializeParameters((short) bits, targetFileSize, 0, verifiability);


            dbSemaphore.acquire();
            StoredFile storedFile = new StoredFile(FILE_PATH, MODULUS, SHAREHOLDERS, NEEDED_SHARES, targetFileSize, bits);
            storedFileDao.createIfNotExists(storedFile);

            CloseableIterator<ShareHolder> iterator = shareholdersDao.closeableIterator();
            try {
                int j = 0;
                shareHolders = new ShareHolder[SHAREHOLDERS];
                while (iterator.hasNext() && j < SHAREHOLDERS) {
                    ShareHolder shareHolder = iterator.next();
                    shareHolders[j] = shareHolder;
                    ManyToMany manyToMany = new ManyToMany(shareHolder, storedFile, BigInteger.valueOf(j+1));
                    manyToManyDao.createIfNotExists(manyToMany);
                    j++;
                }
            } finally {
                // close it at the end to close underlying SQL statement
                iterator.close();
            }
            dbSemaphore.release();

            if (verifiability) {
                BigIntegerPolynomial.g = new BigInteger(Parameters.MOD_LENGTH, new Random()).mod(MODULUS);
                BigIntegerPolynomial.h = new BigInteger(Parameters.MOD_LENGTH, new Random()).mod(MODULUS);
            }

            ExecutorService socketPool = SSLClient.openNewConnections(mode);

            for (int i = 0; i<SHAREHOLDERS; i++){
                sslClients[i].xValue = i + 1;
            }
            /*BigInteger[] xValues = new BigInteger[NEEDED_SHARES];
            for (int i = 0; i < NEEDED_SHARES; i++) {
                xValues[i] = BigInteger.valueOf(i+1);
            }

            //compute Lagrange Coefficients
            BigIntegerPolynomial.computeLagrangeCoefficients(xValues, MODULUS);

            /*BigInteger number = BigInteger.valueOf(144);

            //encrypt
            BigIntegerPolynomial polynomial = new BigIntegerPolynomial(NEEDED_SHARES - 1, MODULUS, number);

            for (int x = 0; x < SHAREHOLDERS; x++) {
                BigInteger xValue = BigInteger.valueOf(x + 1);
                BigInteger yValue = polynomial.evaluatePolynom(xValue);

                BigInteger shareCommitment = polynomial.G.evaluatePolynom(xValue);

                boolean status = BigIntegerPolynomial.verifyCommitment(xValue, yValue, shareCommitment, polynomial.commitments, MODULUS);
                show(status);
            }*/

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
                    byte[][] taskBuffer = (byte[][]) futures[i].get();
                    encrypted += taskBuffer[0].length;

                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        sslClients[j].queue.put(taskBuffer[j]);
                    }

                }
            }
            if (encrypted != SHARES_FILE_SIZE) {
                show("ERROR in File Encryption");
            } else {
                show("File Encryption completed successfully");
            }
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);

            socketPool.shutdown();
            socketPool.awaitTermination(10, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void decryptFile(int mode) {
        try {
            dbSemaphore.acquire();
            StoredFile storedFile = storedFileDao.queryForId(FILE_PATH);
            shareHolders = new ShareHolder[SHAREHOLDERS];
            shareHolders = lookupShareHoldersForStoredFile(storedFile).toArray(shareHolders);
            dbSemaphore.release();

            NEEDED_SHARES = storedFile.getNeededShares();
            MODULUS = storedFile.getModulus();
            initializeParameters((short)storedFile.getBits(), storedFile.getFileSize(), 1, false);

            ExecutorService socketPool = SSLClient.openNewConnections(mode);

            BigInteger[] xValues = new BigInteger[NEEDED_SHARES];

            for (int i = 0; i< NEEDED_SHARES; i++){
                while (sslClients[i].xValue == 0){
                    Thread.sleep(100);
                }
                xValues[i] = BigInteger.valueOf(sslClients[i].xValue);
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
                        buffer[j] = sslClients[j].queue.poll(10, TimeUnit.MINUTES);
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

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private static void addShareHolder(){
        try{
            dbSemaphore.acquire();
            for (int i = 1; i<=10; i++){
                ShareHolder shareHolder = new ShareHolder("localhost"+i, 8000+i);
                shareholdersDao.createIfNotExists(shareHolder);
            }
            dbSemaphore.release();

        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
