package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
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

        SSLClient.openNewConnections(input);

        switch (input) {
            case 1:
                encryptFile();
                break;
            case 2:
                decryptFile();
                break;
        }



        /*Timestamp start = new Timestamp(System.currentTimeMillis());



        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());*/

    }

    public static void encryptFile() {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILE_PATH, "r");
            sourceFile.seek(0L);
            long targetFileSize = sourceFile.length();

            initializeParameters((short) 1024,  targetFileSize, 0);

            try {
                FileOutputStream fout = new FileOutputStream("properties.dat");
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeInt(SHAREHOLDERS);
                oos.writeInt(NEEDED_SHARES);
                oos.writeObject(FILE_PATH);
                oos.writeLong(TARGET_FILE_SIZE);
                oos.writeShort(BITS);
                oos.writeObject(MODULUS);
                oos.close();
            }
            catch (Exception e) { e.printStackTrace(); }

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            long encrypted = 0;
            long processed = 0;
            byte buffer[];
            Future[] futures = new Future[THREADS];

            int numbers = BUFFER_SIZE / BLOCK_SIZE;
            int encryptedBufferSize = numbers*SHARE_SIZE;
            int numberOfThreads = 0;
            boolean lastBuffer = false;
            while (processed < targetFileSize) {
                long previousProcessed = processed;
                for (int i = 0; i < THREADS; i++) {
                    if (targetFileSize - processed >= BUFFER_SIZE) {
                        buffer = new byte[BUFFER_SIZE];
                    } else {
                        buffer = new byte[(int) (targetFileSize - processed)];
                    }

                    sourceFile.readFully(buffer);
                    EncryptionTask task = new EncryptionTask(buffer);
                    futures[i] = pool.submit(task);
                    processed += buffer.length;
                    numberOfThreads = i;
                    if (buffer.length < BUFFER_SIZE){
                        lastBuffer = true;
                        break;
                    }
                }

                numbers = (int) Math.ceil((processed - previousProcessed) / BLOCK_SIZE);
                if (lastBuffer) numbers++;

                byte[][] encryptedData = new byte[SHAREHOLDERS][numbers * SHARE_SIZE];

                for (int i = 0; i <= numberOfThreads; i++) {
                    byte[][] taskBuffer = (byte[][]) futures[i].get();

                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        System.arraycopy(taskBuffer[j], 0, encryptedData[j], i*encryptedBufferSize, taskBuffer[j].length);
                    }
                }

                encrypted += encryptedData[0].length;

                for (int j = 0; j < SHAREHOLDERS; j++) {
                    sslClients[j].queue.put(encryptedData[j]);
                }

            }
            if (encrypted != SHARES_FILE_SIZE_WITHOUT_HEADER){
                show("ERROR");
            }
            show("File Encryption completed");
            pool.shutdown();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void decryptFile() {
        try {

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
            }
            catch (Exception e) { e.printStackTrace(); }
            initializeParameters(BITS, TARGET_FILE_SIZE, 1);

            Thread.sleep(200);
            BigInteger[] xValues = new BigInteger[NEEDED_SHARES];
            for (int i = 0; i < NEEDED_SHARES; i++) {
                xValues[i] = BigInteger.valueOf(sslClients[i].xValue);
            }

            //compute Lagrange Coefficients
            BigIntegerPolynomial.computeLagrangeCoefficients(xValues, MODULUS);

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long destStartingByte = 0;

            long processed = 0;
            byte buffer[][] = new byte[NEEDED_SHARES][];

            Future[] futures = new Future[THREADS];
            while (processed < SHARES_FILE_SIZE_WITHOUT_HEADER) {
                int numberOfThreads = 0;
                for (int i = 0; i < THREADS; i++) {
                    for (int j = 0; j< NEEDED_SHARES; j++){
                        buffer[j] = sslClients[j].queue.poll(10, TimeUnit.MINUTES);
                    }
                    //show(buffer[0].length);
                    int numbers = buffer[0].length / SHARE_SIZE;

                    DecryptionTask task = new DecryptionTask(buffer, destStartingByte);
                    futures[i]= pool.submit(task);
                    destStartingByte += numbers*BLOCK_SIZE;
                    processed += buffer[0].length;
                    numberOfThreads = i;
                    if (processed >=SHARES_FILE_SIZE_WITHOUT_HEADER ) break;
                }

                for (int i = 0; i <= numberOfThreads; i++) {
                    int returnValue = (int) futures[i].get();
                    if (returnValue != 0) show("Error");
                }
            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
