package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public final class Constants {
    // Encryption Parameters
    public static final int SHAREHOLDERS = 5;
    public static final int NBITS = 512; // in bits, must be power of 2
    public static final int BLOCKSIZE = NBITS / 8; // in Bytes
    public static final int MODLENGTH = NBITS + 8;  // in bits, must be power of 2 or = 0 mod 8
    public static final int MODSIZE = MODLENGTH / 8;
    public static final int SHARESIZE = (MODSIZE + 1); // in Bytes
    // Read/Write Parameters
    public static final int NTHREADS = 8;
    public static final int SIZE = 1024 * 1024 * 512;
    public static BigInteger MODULUS = new BigInteger(MODLENGTH, 100000, new Random());

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static BigInteger toPositive(BigInteger num) {
        return num.andNot(BigInteger.valueOf(-1).shiftLeft(MODLENGTH));
    }

}
