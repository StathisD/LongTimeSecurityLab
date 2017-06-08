package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {

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

    static void setMODULUS(BigInteger MODULUS) {
        Parameters.MODULUS = MODULUS;
    }

    public static byte[] fixLength(byte[] data, int length) {

        byte[] newData = new byte[length];
        //add Padding if needed
        if (data.length < length) {
            System.arraycopy(data, 0, newData, (newData.length - data.length), data.length);
        } else if (data.length > length) {
            System.arraycopy(data, data.length - length, newData, 0, length);
        } else {
            newData = data;
        }
        return newData;
    }

    public static void show(Object o) {
        System.out.println(o.toString());
    }

}
