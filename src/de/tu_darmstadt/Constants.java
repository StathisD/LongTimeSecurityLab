package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public final class Constants {
    public static final int SHAREHOLDERS = 4;
    public static final int NBITS = 256;
    public static final int MODLENGTH = NBITS * 2;
    // Read/Write Parameters
    public static final int NCHUNKS = 8;
    public static final int CHUNKSIZE = 4096; // in Bytes, must be power of 2
    public static final int SIZE = NCHUNKS * CHUNKSIZE;
    public static BigInteger MODULUS = new BigInteger(MODLENGTH, 100000, new Random());
}
