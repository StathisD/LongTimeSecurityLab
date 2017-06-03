package de.tu_darmstadt;

/**
 * Created by stathis on 6/3/17.
 */
public final class Constants {
    public static final int NBITS = 128;
    public static final int NCHUNKS = 8;
    public static final int CHUNKSIZE = 4096; // in Bytes, must be power of 2
    public static final int SIZE = NCHUNKS * CHUNKSIZE;
}
