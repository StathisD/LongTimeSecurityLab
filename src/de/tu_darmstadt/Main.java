package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;


public class Main {


    public static void main(String[] args) {

        FILE_PATH = args[0];
        SHAREHOLDERS = Integer.parseInt(args[1]);
        NEEDED_SHARES = Integer.parseInt(args[2]);

        //SSLServer.startServer();
        SSLClient.startClient();

        /*Timestamp start = new Timestamp(System.currentTimeMillis());

        encrypt();
        decrypt();

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());*/

    }

    public static void encrypt() {
        try {
            RandomAccessFile in = new RandomAccessFile(FILE_PATH, "r");
            in.seek(0L);
            long targetFileSize = in.length();

            initializeParameters((short) 1024, 14, targetFileSize, 0);

            RandomAccessFile[] outs = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < SHAREHOLDERS; j++) {
                outs[j] = new RandomAccessFile(FILE_PATH + j, "rw");
                outs[j].seek(0L);
                outs[j].writeLong(TARGET_FILE_SIZE);
                outs[j].writeShort(BITS);
                outs[j].writeInt(j + 1);
                outs[j].write(fixLength(MODULUS.toByteArray(), SHARE_SIZE));
            }
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long sourceStartingByte;
            long sourceEndingByte;
            long destStartingByte = HEADER_LENGTH;

            for (int i = 0; i < THREADS; i++) {

                if (i < THREADS - 1) {
                    sourceStartingByte = i * CHUNK_SIZE;
                    sourceEndingByte = (i + 1) * CHUNK_SIZE;
                } else {
                    sourceStartingByte = i * CHUNK_SIZE;
                    sourceEndingByte = TARGET_FILE_SIZE;
                }
                System.out.println("Starting Encryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                System.out.println("Starting Encryption Task with dest: " + destStartingByte);

                EncryptionTask task = new EncryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                pool.submit(task);

                long numbers = (sourceEndingByte - sourceStartingByte) / BLOCK_SIZE;
                destStartingByte += numbers * SHARE_SIZE;

            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);
            if (outs[0].length() != SHARES_FILE_SIZE_WITH_HEADER) {
                System.out.println("ERROR in Encryption");
                System.out.println(SHARES_FILE_SIZE_WITH_HEADER - outs[0].length());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void decrypt() {
        try {
            RandomAccessFile in = new RandomAccessFile(FILE_PATH + 0, "r");
            in.seek(0L);
            long targetFileSize = in.readLong();
            short nbits = in.readShort();

            initializeParameters(nbits, 14, targetFileSize, 1);
            BigInteger[] xValues = new BigInteger[NEEDED_SHARES];
            for (int i = 0; i < NEEDED_SHARES; i++) {
                in = new RandomAccessFile(FILE_PATH + i, "r");
                in.seek(10);
                xValues[i] = BigInteger.valueOf(in.readInt());
            }
            byte[] bytes = new byte[SHARE_SIZE];
            in.readFully(bytes);
            BigInteger modulus = new BigInteger(1, bytes);
            setMODULUS(modulus);
            in.seek(0L);

            //compute Lagrange Coefficients
            BigIntegerPolynomial.computeLagrangeCoefficients(xValues, MODULUS);

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long sourceStartingByte;
            long sourceEndingByte;
            long destStartingByte = 0;

            for (int i = 0; i < THREADS; i++) {

                if (i < THREADS - 1) {
                    sourceStartingByte = i * CHUNK_SIZE + HEADER_LENGTH;
                    sourceEndingByte = (i + 1) * CHUNK_SIZE + HEADER_LENGTH;
                } else {
                    sourceStartingByte = i * CHUNK_SIZE + HEADER_LENGTH;
                    sourceEndingByte = SHARES_FILE_SIZE_WITH_HEADER;
                }
                show("Starting Decryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                show("Starting Decryption Task with dest: " + destStartingByte);

                DecryptionTask task = new DecryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                pool.submit(task);

                long numbers = (sourceEndingByte - sourceStartingByte) / SHARE_SIZE;
                destStartingByte += numbers * BLOCK_SIZE;

            }
            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
