package de.tu_darmstadt;

import de.tu_darmstadt.Decryption.DecryptionTask;
import de.tu_darmstadt.Encryption.EncryptionTask;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;

/**
 * Created by Stathis on 6/9/17.
 */
public class ExampleCode {
    public static void encrypt() {
        try {
            RandomAccessFile in = new RandomAccessFile(FILE_PATH, "r");
            in.seek(0L);
            long targetFileSize = in.length();

            //initializeParameters((short) 1024, 14, targetFileSize, 0);


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

                //EncryptionTask task = new EncryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                //pool.submit(task);

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

            //initializeParameters(nbits, 14, targetFileSize, 1);
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

               // DecryptionTask task = new DecryptionTask(sourceStartingByte, sourceEndingByte, destStartingByte);
                //pool.submit(task);

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

class EncryptionTaskExample implements Runnable {

    private long sourceStartingByte;
    private long sourceEndingByte;
    private long destStartingByte;

    public EncryptionTaskExample(long sourceStartingByte, long sourceEndingByte, long destStartingByte) {
        this.sourceStartingByte = sourceStartingByte;
        this.sourceEndingByte = sourceEndingByte;
        this.destStartingByte = destStartingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILE_PATH, "r");
            RandomAccessFile targetFiles[] = new RandomAccessFile[SHAREHOLDERS];
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;

            sourceFile.seek(sourceStartingByte);

            for (int j = 0; j < SHAREHOLDERS; j++) {
                targetFiles[j] = new RandomAccessFile(FILE_PATH + j, "rw");
                targetFiles[j].seek(destStartingByte);
            }

            while (processed < contentLength) {
                byte buffer[];
                if (contentLength - processed >= BUFFER_SIZE) {
                    buffer = new byte[BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (contentLength - processed)];

                }

                sourceFile.readFully(buffer);
                int numbersInBuffer = (int) Math.ceil(buffer.length * 1.0 / BLOCK_SIZE);
                byte[][] encryptedData = new byte[SHAREHOLDERS][numbersInBuffer * SHARE_SIZE];
                byte[] oneNumber;

                for (int i = 0; i < numbersInBuffer; i++) {
                    if ((i + 1) * BLOCK_SIZE <= buffer.length) {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCK_SIZE, (i + 1) * BLOCK_SIZE);
                    } else {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCK_SIZE, buffer.length);
                    }

                    BigInteger number = new BigInteger(1, oneNumber);

                    //encrypt
                    BigIntegerPolynomial polynomial = new BigIntegerPolynomial(NEEDED_SHARES - 1, MODULUS, number);

                    byte[] byteShare;

                    for (int x = 0; x < SHAREHOLDERS; x++) {
                        BigInteger xValue = BigInteger.valueOf(x + 1);
                        BigInteger yValue = polynomial.evaluate(xValue);
                        byteShare = fixLength(yValue.toByteArray(), SHARE_SIZE);
                        System.arraycopy(byteShare, 0, encryptedData[x], i * SHARE_SIZE, byteShare.length);
                    }
                }
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    targetFiles[j].write(encryptedData[j]);
                }
                processed += buffer.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

final class ParametersExample {

    // Encryption Parameters
    public static int SHAREHOLDERS;
    public static int NEEDED_SHARES;
    public static int BLOCK_SIZE; // in Bytes
    public static int SHARE_SIZE; // in Bytes
    public static BigInteger MODULUS;
    public static long TARGET_FILE_SIZE;
    public static long SHARES_FILE_SIZE_WITH_HEADER;
    public static int BUFFER_SIZE;
    public static String FILE_PATH;
    static short BITS; // in bits, must be power of 2
    static int MOD_LENGTH; // in bits, must be power of 2 or = 0 mod 8
    // Read/Write Parameters
    static int THREADS;
    static int HEADER_LENGTH;
    static long SHARES_FILE_SIZE_WITHOUT_HEADER;
    static long CHUNK_SIZE;
    static int MAX_BUFFER_SIZE = 1024 * 1024 * 20; // 100 for Encryption

    static void initializeParameters(short BITS, int HEADER, long TARGET_FILE_SIZE, int mode) {
        Parameters.BITS = BITS;
        Parameters.BLOCK_SIZE = BITS / 8;
        Parameters.MOD_LENGTH = BITS + 8;
        Parameters.SHARE_SIZE = MOD_LENGTH / 8;
        Parameters.THREADS = Runtime.getRuntime().availableProcessors();
        Parameters.HEADER_LENGTH = HEADER + SHARE_SIZE;
        Parameters.TARGET_FILE_SIZE = TARGET_FILE_SIZE;
        long x = (long) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCK_SIZE);
        Parameters.SHARES_FILE_SIZE_WITHOUT_HEADER = x * SHARE_SIZE;
        Parameters.SHARES_FILE_SIZE_WITH_HEADER = x * SHARE_SIZE + HEADER_LENGTH;
        long chunkSize;
        int maxPossibleBuffer;
        int numberSize;
        if (mode == 0) {
            byte[] bytes = new byte[BITS / 8];
            Arrays.fill(bytes, (byte) 0xff);
            BigInteger number = new BigInteger(1, bytes);
            MODULUS = new BigInteger(MOD_LENGTH, 100000, new Random());
            while (number.compareTo(MODULUS) > 0) {
                MODULUS = new BigInteger(MOD_LENGTH, 100000, new Random());
            }
            chunkSize = TARGET_FILE_SIZE;
            numberSize = BLOCK_SIZE;
        } else {
            chunkSize = SHARES_FILE_SIZE_WITHOUT_HEADER;
            numberSize = SHARE_SIZE;
        }
        chunkSize = (long) Math.ceil(chunkSize / (THREADS * 1.0));
        if (MAX_BUFFER_SIZE >= chunkSize) {
            Parameters.CHUNK_SIZE = chunkSize - (chunkSize % numberSize);
            Parameters.BUFFER_SIZE = (int) CHUNK_SIZE;
        } else {
            maxPossibleBuffer = MAX_BUFFER_SIZE - MAX_BUFFER_SIZE % numberSize;
            Parameters.CHUNK_SIZE = chunkSize - (chunkSize % maxPossibleBuffer);
            Parameters.BUFFER_SIZE = (int) Math.min(maxPossibleBuffer, CHUNK_SIZE);
        }
    }
}



