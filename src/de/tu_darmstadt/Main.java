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

        //enc();
        dec();

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }

    public static void enc() {
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
            ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);

            long sourceStartingByte = 0;
            long sourceEndingByte = 0;
            long destStartingByte = HEADER_LENGTH;
            long destEndingByte = HEADER_LENGTH;

            int x = TARGET_CHUNK_SIZE / BLOCKSIZE;
            SHARES_CHUNK_SIZE = SHARESIZE * x;

            System.out.println(TARGET_CHUNK_SIZE % BLOCKSIZE);
            System.out.println(SHARES_CHUNK_SIZE % SHARESIZE);

            while (TARGET_FILE_SIZE > sourceStartingByte) {

                if (sourceEndingByte + TARGET_CHUNK_SIZE <= TARGET_FILE_SIZE) {
                    sourceEndingByte = sourceEndingByte + TARGET_CHUNK_SIZE;

                } else {
                    sourceEndingByte = TARGET_FILE_SIZE;
                }
                destEndingByte = destEndingByte + SHARES_CHUNK_SIZE;


                System.out.println("Starting Encryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                System.out.println("Starting Encryption Task with dest: " + destStartingByte + " to " + destEndingByte);
                System.out.println();

                EncryptionTask task = new EncryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte, destEndingByte);
                pool.submit(task);

                sourceStartingByte = sourceEndingByte;
                destStartingByte = destEndingByte;

            }

            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);
            if (outs[0].length() != SHARES_FILE_SIZE + HEADER_LENGTH) {
                System.out.println("ERROR");
                System.out.println(SHARES_FILE_SIZE + HEADER_LENGTH - outs[0].length());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void dec() {
        try {

            RandomAccessFile[] ins = new RandomAccessFile[5];

            int j = 0;
            ins[j] = new RandomAccessFile(FILEPATH + j, "r");
            ins[j].seek(0L);
            long targetFileSize = ins[j].readLong();
            short nbits = ins[j].readShort();
            short shareholders = ins[j].readShort();
            initializeParameters(shareholders, nbits, (short) 8, 12, 1024 * 1024 * 1024, targetFileSize, 1);
            byte[] bytes = new byte[MODSIZE];
            ins[j].readFully(bytes);
            BigInteger modulus = new BigInteger(1, bytes);
            setMODULUS(modulus);
            ins[j].seek(0L);

            ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);

            long sourceStartingByte = HEADER_LENGTH;
            long sourceEndingByte = HEADER_LENGTH;
            long destStartingByte = 0;
            long destEndingByte = 0;

            int x = SHARES_CHUNK_SIZE / BLOCKSIZE;
            TARGET_CHUNK_SIZE = BLOCKSIZE * x;

            System.out.println(SHARES_FILE_SIZE % SHARES_CHUNK_SIZE);


            while (SHARES_FILE_SIZE > sourceStartingByte) {

                if (sourceEndingByte + SHARES_CHUNK_SIZE <= SHARES_FILE_SIZE) {
                    sourceEndingByte = sourceEndingByte + SHARES_CHUNK_SIZE;

                } else {
                    sourceEndingByte = SHARES_FILE_SIZE;
                }

                destEndingByte = destEndingByte + TARGET_CHUNK_SIZE;


                System.out.println("Starting Decryption Task with source: " + sourceStartingByte + " to " + sourceEndingByte);
                //System.out.println("Starting Decryption Task with dest: " + destStartingByte +" to " + destEndingByte);

                show((sourceEndingByte - sourceStartingByte));
                System.out.println();
                DecryptionTask task = new DecryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte, destEndingByte);
                pool.submit(task);

                sourceStartingByte = sourceEndingByte;
                destStartingByte = destEndingByte;

            }
            pool.shutdown();
            pool.awaitTermination(60 * 15, TimeUnit.SECONDS);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
