package de.tu_darmstadt;

import com.j256.ormlite.dao.Dao;

import java.math.BigInteger;
import java.util.concurrent.Semaphore;


/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {
    public static int SERVER_NR;
    public static String SERVER_NAME;
    public static  String SHARE_DIR;
    // Encryption Parameters
    public static BigInteger MODULUS;
    public static int BUFFER_SIZE;
    public static int SHARE_SIZE;
    public static boolean VERIFIABILITY;
    public static int BLOCK_SIZE; // in Bytes
    public static long timeSlot = 1000*10;
    public static PedersenParameters pedersenParameters;
    public static Dao<ShareHolder, String> shareholdersDao;
    public static Dao<Share, String> sharesDao;
    public static Dao<ManyToMany, String> manyToManyDao;
    public static Dao<PedersenParameters, String> pedersenParametersDao;
    public static Semaphore dbSemaphore;
    static int MOD_LENGTH; // in bits, must be power of 2 or = 0 mod 8
    static PedersenCommitter committer;
    // Read/Write Parameters
    static int ports[];
    static int THREADS;
    private static int MAX_BUFFER_SIZE = 1024 * 1024 * 1;

    static void initializeParameters(long TARGET_FILE_SIZE, int mode, boolean verifiability) {
        Parameters.VERIFIABILITY = verifiability;
        Parameters.BLOCK_SIZE = pedersenParameters.encodingBits / 8;
        Parameters.MOD_LENGTH = pedersenParameters.encodingBits + 8;
        Parameters.SHARE_SIZE = MOD_LENGTH / 8;
        Parameters.THREADS = Runtime.getRuntime().availableProcessors();
        long x = (long) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCK_SIZE);
        long SHARES_FILE_SIZE = x * SHARE_SIZE;

        int numberSize;
        long fileSize;
        if (mode == 0) {
            numberSize = BLOCK_SIZE;
            fileSize = TARGET_FILE_SIZE;
        } else {
            numberSize = SHARE_SIZE;
            fileSize = SHARES_FILE_SIZE;
        }

        BUFFER_SIZE = (int) Math.min(Math.ceil(fileSize / THREADS), MAX_BUFFER_SIZE);
        BUFFER_SIZE = BUFFER_SIZE - BUFFER_SIZE % numberSize;

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
