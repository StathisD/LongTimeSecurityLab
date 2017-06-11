package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;


public class Main {


    public static void main(String[] args) {

        FILE_PATH = args[0];
        SHAREHOLDERS = Integer.parseInt(args[1]);
        NEEDED_SHARES = Integer.parseInt(args[2]);

        Scanner scanner = new Scanner(System.in);
        int input;
        do {
            show("Please specify what you want to do:");
            show("(Type 1 for sharing a new file)");
            show("(Type 2 for decrypting a File)");
            input = scanner.nextInt();
        } while (input <= 0 || input > 2);

        Timestamp start = new Timestamp(System.currentTimeMillis());

        switch (input) {
            case 1:
                encryptFile(input);
                break;
            case 2:
                decryptFile(input);
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

            boolean verifiability = true;

            initializeParameters((short) 1024, targetFileSize, 0, verifiability);

            if (verifiability) {
                BigIntegerPolynomial.g = new BigInteger(Parameters.MOD_LENGTH, new Random()).mod(MODULUS);
                BigIntegerPolynomial.h = new BigInteger(Parameters.MOD_LENGTH, new Random()).mod(MODULUS);
            }

            FileOutputStream fout = new FileOutputStream("properties.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeInt(SHAREHOLDERS);
            oos.writeInt(NEEDED_SHARES);
            oos.writeObject(FILE_PATH);
            oos.writeLong(TARGET_FILE_SIZE);
            oos.writeShort(BITS);
            oos.writeObject(MODULUS);
            oos.close();

            ExecutorService socketPool = SSLClient.openNewConnections(mode);

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
            FileInputStream fin = new FileInputStream("properties.dat");
            ObjectInputStream ois = new ObjectInputStream(fin);
            SHAREHOLDERS = ois.readInt();
            NEEDED_SHARES = ois.readInt();
            FILE_PATH = (String) ois.readObject();
            TARGET_FILE_SIZE = ois.readLong();
            BITS = ois.readShort();
            MODULUS = (BigInteger) ois.readObject();
            ois.close();

            initializeParameters(BITS, TARGET_FILE_SIZE, 1, false);

            Thread.sleep(200);
            BigInteger[] xValues = new BigInteger[NEEDED_SHARES];
            for (int i = 0; i < NEEDED_SHARES; i++) {
                xValues[i] = BigInteger.valueOf(sslClients[i].xValue);
            }

            //compute Lagrange Coefficients
            BigIntegerPolynomial.computeLagrangeCoefficients(xValues, MODULUS);

            ExecutorService socketPool = SSLClient.openNewConnections(mode);

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long destStartingByte = 0;
            long processed = 0;
            int numberOfThreads = 0;
            byte buffer[][] = new byte[NEEDED_SHARES][];
            Future[] futures = new Future[THREADS];

            while (processed < SHARES_FILE_SIZE) {

                for (int i = 0; i < THREADS; i++) {

                    for (int j = 0; j< NEEDED_SHARES; j++){
                        buffer[j] = sslClients[j].queue.poll(10, TimeUnit.MINUTES);
                    }
                    int numbers = buffer[0].length / SHARE_SIZE;

                    DecryptionTask task = new DecryptionTask(buffer, destStartingByte);
                    futures[i]= pool.submit(task);
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
}
