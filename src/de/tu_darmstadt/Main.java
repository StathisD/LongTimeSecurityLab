package de.tu_darmstadt;

import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Constants.*;


public class Main {


    public static void main(String[] args) {
        byte[] bytes = new byte[NBITS / 8];
        Arrays.fill(bytes, (byte) 0xff);
        BigInteger number = new BigInteger(1, bytes);
        while (number.compareTo(MODULUS) > 0) {
            MODULUS = new BigInteger(MODLENGTH, 100000, new Random());
        }

        Timestamp start = new Timestamp(System.currentTimeMillis());

        encrypt(args[0]);

        /*String[] paths = new String[SHAREHOLDERS];
        for (int j = 0; j < SHAREHOLDERS; j++) {
            paths[j] = "/media/stathis/9AEA2384EA235BAF/testFile" +j;
        }
        decrypt(paths);*/

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }

    public static void encrypt(String path) {


        ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);
        try {
            RandomAccessFile in = new RandomAccessFile(path, "r");
            in.seek(0L);

            RandomAccessFile[] outs = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < SHAREHOLDERS; j++) {
                outs[j] = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/testFile" + j, "rw");
                outs[j].seek(0L);
            }

            int nGet;
            while (in.getFilePointer() < in.length()) {
                Future<byte[][]>[] futures = new Future[NTHREADS];
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
                        EncryptionTask task = new EncryptionTask(chunk);
                        futures[i] = pool.submit(task);

                    } else {
                        byte[] chunk = new byte[chunkSize];
                        System.arraycopy(byteArray, i * chunkSize, chunk, 0, chunkSize);
                        EncryptionTask task = new EncryptionTask(chunk);
                        futures[i] = pool.submit(task);
                    }
                }
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
                /*for (int j = 0; j < SHAREHOLDERS; j++) {
                    outs[j].write(encryptedData[j]);
                }*/
            }

        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();

        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        } finally {
            pool.shutdown();
        }
    }

    /*public static void decrypt(String[] paths) {


        ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);
        try {

            RandomAccessFile[] ins = new RandomAccessFile[SHAREHOLDERS];
            for (int j = 0; j < SHAREHOLDERS; j++) {
                ins[j] = new RandomAccessFile(paths[j], "r");
                ins[j].seek(0L);
            }

            RandomAccessFile out = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/testFile_dec", "rw");


            int nGet;
            while (in.getFilePointer() < in.length()) {
                Future<byte[][]>[] futures = new Future[NTHREADS];
                nGet = (int) Math.min(SIZE, in.length() - in.getFilePointer());
                final byte[] byteArray = new byte[nGet];
                in.readFully(byteArray);
                int chunkSize = nGet/NTHREADS;
                int remain = nGet % NTHREADS;
                // process byteArray
                for (int i = 0; i < NTHREADS; i++) {
                    if (nGet < (i + 1) * chunkSize) {
                        byte[] chunk = new byte[remain];
                        System.arraycopy(byteArray, i * chunkSize, chunk, 0, remain);
                        EncryptionTask task = new EncryptionTask(chunk);
                        futures[i] = pool.submit(task);

                    } else {
                        byte[] chunk = new byte[chunkSize];
                        System.arraycopy(byteArray, i * chunkSize, chunk, 0, chunkSize);
                        EncryptionTask task = new EncryptionTask(chunk);
                        futures[i] = pool.submit(task);
                    }
                }
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
                /*for (int j = 0; j < SHAREHOLDERS; j++) {
                    outs[j].write(encryptedData[j]);
                }*/
           /* }

        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();

        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
        } finally {
            pool.shutdown();
        }
    }*/
}
