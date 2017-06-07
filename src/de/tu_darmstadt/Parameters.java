package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {

    // Encryption Parameters
    public static short SHAREHOLDERS;
    public static short NBITS; // in bits, must be power of 2
    public static int BLOCKSIZE; // in Bytes
    public static int MODLENGTH; // in bits, must be power of 2 or = 0 mod 8
    public static int MODSIZE; // in Bytes
    public static int SHARESIZE; // in Bytes
    public static BigInteger MODULUS;

    // Read/Write Parameters
    public static short NTHREADS;
    public static int HEADER_LENGTH;
    public static int CHUNKOFFILE;
    public static long TARGET_FILE_SIZE;
    public static long SHARES_FILE_SIZE_WITHOUT_HEADER;
    public static long SHARES_FILE_SIZE_WITH_HEADER;
    public static int CHUNK_SIZE;
    public static int MAX_BUFFER_SIZE = 1024 * 1024 * 100;
    public static int BUFFER_SIZE;
    public static String FILEPATH;

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static void initializeParameters(short SHAREHOLDERS, short NBITS, short NTHREADS, int HEADER, int CHUNKOFFILE, long TARGET_FILE_SIZE, int mode) {
        Parameters.SHAREHOLDERS = SHAREHOLDERS;
        Parameters.NBITS = NBITS;
        Parameters.BLOCKSIZE = NBITS / 8;
        Parameters.MODLENGTH = NBITS + 8;
        Parameters.MODSIZE = MODLENGTH / 8;
        Parameters.SHARESIZE = MODSIZE + 1;
        Parameters.NTHREADS = NTHREADS;
        Parameters.HEADER_LENGTH = HEADER + MODSIZE;
        Parameters.CHUNKOFFILE = CHUNKOFFILE;
        Parameters.TARGET_FILE_SIZE = TARGET_FILE_SIZE;
        long x = (long) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCKSIZE);
        Parameters.SHARES_FILE_SIZE_WITHOUT_HEADER = x * SHARESIZE;
        Parameters.SHARES_FILE_SIZE_WITH_HEADER = x * SHARESIZE + HEADER_LENGTH;
        int temp;
        if (mode == 0) {
            byte[] bytes = new byte[NBITS / 8];
            Arrays.fill(bytes, (byte) 0xff);
            BigInteger number = new BigInteger(1, bytes);
            MODULUS = new BigInteger(MODLENGTH, 100000, new Random());
            while (number.compareTo(MODULUS) > 0) {
                MODULUS = new BigInteger(MODLENGTH, 100000, new Random());
            }
            temp = (int) Math.min(CHUNKOFFILE, TARGET_FILE_SIZE) / NTHREADS;
        } else {
            temp = (int) Math.min(CHUNKOFFILE, SHARES_FILE_SIZE_WITHOUT_HEADER) / NTHREADS;
        }

        int maxPossibleBuffer = Math.min(MAX_BUFFER_SIZE, temp);
        int a = maxPossibleBuffer / (SHARESIZE * BLOCKSIZE);
        Parameters.BUFFER_SIZE = a * (SHARESIZE * BLOCKSIZE);
        int y = temp / Parameters.BUFFER_SIZE;
        Parameters.CHUNK_SIZE = y * BUFFER_SIZE;

        show(BUFFER_SIZE);
        show(CHUNK_SIZE);
    }

    public static void setMODULUS(BigInteger MODULUS) {
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
