package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {
    // Encryption Parameters
    public static short SHAREHOLDERS = 5;
    public static short NBITS = 512; // in bits, must be power of 2
    public static int BLOCKSIZE = NBITS / 8; // in Bytes
    public static int MODLENGTH = NBITS + 8;  // in bits, must be power of 2 or = 0 mod 8
    public static int MODSIZE = MODLENGTH / 8; // in Bytes
    public static int SHARESIZE = (MODSIZE + 1); // in Bytes
    public static BigInteger MODULUS = new BigInteger(MODLENGTH, 100000, new Random());

    // Read/Write Parameters
    public static short NTHREADS = 8;
    public static int SIZE = 1024 * 1024 * 60;
    public static long FILESIZE;

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static void initializeParameters(short SHAREHOLDERS, short NBITS, short NTHREADS, int SIZE, long FILESIZE) {
        Parameters.SHAREHOLDERS = SHAREHOLDERS;
        Parameters.NBITS = NBITS;
        Parameters.BLOCKSIZE = NBITS / 8;
        Parameters.MODLENGTH = NBITS + 8;
        Parameters.MODSIZE = Parameters.MODLENGTH / 8;
        Parameters.SHARESIZE = Parameters.MODSIZE + 1;
        Parameters.NTHREADS = NTHREADS;
        Parameters.SIZE = SIZE;
        Parameters.FILESIZE = FILESIZE;
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
            System.arraycopy(data, 1, newData, 0, length);
        } else {
            newData = data;
        }
        return newData;
    }

}
