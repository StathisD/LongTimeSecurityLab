package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;


public class Main {


    public static void main(String[] args) {
        byte[] bytes = new byte[NBITS / 8];
        Arrays.fill(bytes, (byte) 0xff);
        BigInteger number = new BigInteger(1, bytes);
        while (number.compareTo(MODULUS) > 0) {
            MODULUS = new BigInteger(MODLENGTH, 100000, new Random());
        }

        Timestamp start = new Timestamp(System.currentTimeMillis());

        //encrypt(args[0]);

        String[] paths = new String[SHAREHOLDERS];
        for (int j = 0; j < SHAREHOLDERS; j++) {
            paths[j] = "/media/stathis/9AEA2384EA235BAF/testFile" +j;
        }
        decrypt(paths);

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }

    public static void encrypt(String path) {


        ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);
        try {
            RandomAccessFile in = new RandomAccessFile(path, "r");
            in.seek(0L);
            FILESIZE = in.length();

            RandomAccessFile[] outs = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < SHAREHOLDERS; j++) {
                outs[j] = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/testFile" + j, "rw");
                outs[j].seek(0L);
                outs[j].writeLong(FILESIZE);
                outs[j].writeInt(SIZE);
                outs[j].writeShort(NTHREADS);
                outs[j].writeShort(NBITS);
                outs[j].writeShort(SHAREHOLDERS);
                outs[j].write(fixLength(MODULUS.toByteArray(), MODSIZE));
            }

            int nGet;
            while (in.getFilePointer() < in.length()) {
                nGet = (int) Math.min(SIZE, in.length() - in.getFilePointer());
                final byte[] byteArray = new byte[nGet];
                in.readFully(byteArray);
                int chunkSize = nGet / NTHREADS;
                int remain = nGet % NTHREADS;

                // process byteArray
                for (int i = 0; i < NTHREADS; i++) {
                    if (nGet < (i + 1) * chunkSize) {
                        byte[] chunk = new byte[remain];
                        System.arraycopy(byteArray, i * chunkSize, chunk, 0, remain);
                        EncryptionTask task = new EncryptionTask(chunk, i, outs);
                        pool.submit(task);

                    } else {
                        byte[] chunk = new byte[chunkSize];
                        System.arraycopy(byteArray, i * chunkSize, chunk, 0, chunkSize);
                        EncryptionTask task = new EncryptionTask(chunk, i, outs);
                        pool.submit(task);
                    }
                }
            }
            pool.shutdown();
            pool.awaitTermination(60, TimeUnit.SECONDS);


        } catch (InterruptedException ex) {
            ex.printStackTrace();

        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        }
    }

    public static void decrypt(String[] paths) {


        ExecutorService pool = Executors.newFixedThreadPool(1);
        try {
            RandomAccessFile out = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/testFile_dec", "rw");
            RandomAccessFile[] ins = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < paths.length; j++) {
                ins[j] = new RandomAccessFile(paths[j], "r");

                if (j == 0) {
                    ins[j].seek(0L);
                    long fileSize = ins[j].readLong();
                    int size = ins[j].readInt();
                    short nthreads = ins[j].readShort();
                    short nbits = ins[j].readShort();
                    short shareholders = ins[j].readShort();
                    initializeParameters(shareholders, nbits, nthreads, size, fileSize);
                    byte[] bytes = new byte[MODSIZE];
                    ins[j].readFully(bytes);
                    BigInteger modulus = new BigInteger(1, bytes);
                    setMODULUS(modulus);
                } else {
                    ins[j].seek(ins[0].getFilePointer());
                }
            }
            for (int i = 0; i < 1; i++) {
                DecryptionTask task = new DecryptionTask(i, ins, out);
                pool.submit(task);
            }
            pool.shutdown();
            //pool.awaitTermination(60, TimeUnit.SECONDS);


                /*
                int nGet;
                while (ins[j].getFilePointer() < ins[j].length()) {

                    nGet = (int) Math.min(SIZE, ins[j].length() - ins[j].getFilePointer());
                    final byte[] byteArray = new byte[nGet];
                    ins[j].readFully(byteArray);
                    int chunkSize = nGet / NTHREADS;
                    int remain = nGet % NTHREADS;
                    // process byteArray
                    for (int i = 0; i < NTHREADS; i++) {
                        if (nGet < (i + 1) * chunkSize) {
                            byte[] chunk = new byte[remain];
                            System.arraycopy(byteArray, i * chunkSize, chunk, 0, remain);
                            DecryptionTask task = new DecryptionTask(chunk);
                            pool.submit(task);

                        } else {
                            byte[] chunk = new byte[chunkSize];
                            System.arraycopy(byteArray, i * chunkSize, chunk, 0, chunkSize);
                            DecryptionTask task = new DecryptionTask(chunk);
                            pool.submit(task);
                        }
                    }
                    /*
                    //int size = (int) Math.ceil(nGet * 1.0 / (NBITS/8))*SHARESIZE*NCHUNKS;
                    //byte[][] encryptedData = new byte[SHAREHOLDERS][size];
                    int pos = 0;
                    for (int i = 0; i < NTHREADS; i++) {
                        byte[][] returnValue = futures[i].get();
                        //System.out.println("Enc: " + DatatypeConverter.printHexBinary(returnValue[1]));
                        for (int j = 0; j < SHAREHOLDERS; j++) {
                            outs[j].write(returnValue[j]);
                            //System.arraycopy(returnValue[j], 0, encryptedData[j], pos, returnValue[j].length);
                            //pos += returnValue[j].length;
                        }

                    }
                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        outs[j].write(encryptedData[j]);
                    }*/

        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        } finally {
            pool.shutdown();
        }
    }
}
