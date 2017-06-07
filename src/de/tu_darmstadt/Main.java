package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;


public class Main {


    public static void main(String[] args) {
        FILEPATH = args[0];


        Timestamp start = new Timestamp(System.currentTimeMillis());

        //encrypt();
        decrypt();

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }

    public static void encrypt() {
        try {
            RandomAccessFile in = new RandomAccessFile(FILEPATH, "r");
            in.seek(0L);
            long targetFileSize = in.length();

            initializeParameters((short) 5, (short) 512, (short) 8, 12, 1024 * 1024 * 1024, targetFileSize, 0);

            RandomAccessFile[] outs = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < SHAREHOLDERS; j++) {
                outs[j] = new RandomAccessFile(FILEPATH + j, "rw");
                outs[j].seek(0L);
                outs[j].writeLong(TARGET_FILE_SIZE);
                outs[j].writeShort(NBITS);
                outs[j].writeShort(SHAREHOLDERS);
                outs[j].write(fixLength(MODULUS.toByteArray(), MODSIZE));
            }
            ExecutorService pool = Executors.newFixedThreadPool(NTHREADS + 1);

            long sourceStartingByte = 0;
            long sourceEndingByte = 0;

            while (TARGET_FILE_SIZE > sourceStartingByte) {

                if (sourceEndingByte + CHUNK_SIZE <= TARGET_FILE_SIZE) {
                    sourceEndingByte = sourceEndingByte + CHUNK_SIZE;

                } else {
                    sourceEndingByte = TARGET_FILE_SIZE;
                }

                long numbers = sourceStartingByte / BLOCKSIZE;
                long destStartingByte = numbers * SHARESIZE + HEADER_LENGTH;
                numbers = sourceEndingByte / BLOCKSIZE;
                long destEndingByte = numbers * SHARESIZE + HEADER_LENGTH;


                System.out.println("Starting Encryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                System.out.println("Starting Encryption Task with dest: " + destStartingByte + " to " + destEndingByte);

                EncryptionTask task = new EncryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                pool.submit(task);

                sourceStartingByte = sourceEndingByte;
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
            RandomAccessFile in = new RandomAccessFile(FILEPATH + 0, "r");
            in.seek(0L);
            long targetFileSize = in.readLong();
            short nbits = in.readShort();
            short shareholders = in.readShort();
            initializeParameters(shareholders, nbits, (short) 8, 12, 1024 * 1024 * 1024, targetFileSize, 1);
            byte[] bytes = new byte[MODSIZE];
            in.readFully(bytes);
            BigInteger modulus = new BigInteger(1, bytes);
            setMODULUS(modulus);
            in.seek(0L);

            ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);

            long sourceStartingByte = HEADER_LENGTH;
            long sourceEndingByte = HEADER_LENGTH;


            while (SHARES_FILE_SIZE_WITH_HEADER > sourceStartingByte) {

                if (sourceEndingByte + CHUNK_SIZE <= SHARES_FILE_SIZE_WITH_HEADER) {
                    sourceEndingByte = sourceEndingByte + CHUNK_SIZE;
                } else {
                    sourceEndingByte = SHARES_FILE_SIZE_WITH_HEADER;
                }

                long numbers = (sourceStartingByte - HEADER_LENGTH) / SHARESIZE;
                long destStartingByte = numbers * BLOCKSIZE;
                numbers = (sourceEndingByte - HEADER_LENGTH) / SHARESIZE;
                long destEndingByte = numbers * BLOCKSIZE;

                show("Starting Decryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                show("Starting Decryption Task with dest: " + destStartingByte + " to " + destEndingByte);

                DecryptionTask task = new DecryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                pool.submit(task);

                sourceStartingByte = sourceEndingByte;
            }
            pool.shutdown();
            pool.awaitTermination(20, TimeUnit.MINUTES);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
