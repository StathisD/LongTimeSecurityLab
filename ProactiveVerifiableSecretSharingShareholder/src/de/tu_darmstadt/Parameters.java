package de.tu_darmstadt;

import com.j256.ormlite.dao.Dao;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.concurrent.Semaphore;


/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {

    // Encryption Parameters
    public static int SHAREHOLDERS;
    public static int NEEDED_SHARES;
    public static int SHARE_SIZE; // in Bytes
    public static BigInteger MODULUS;
    public static int BUFFER_SIZE;
    static int MOD_LENGTH; // in bits, must be power of 2 or = 0 mod 8
    public static int MOD_SIZE;
    public static boolean VERIFIABILITY;

    public static Dao<ShareHolder, String> shareholdersDao;
    public static Dao<Share, String> sharesDao;
    public static Dao<ManyToMany, String> manyToManyDao;
    public static Semaphore dbSemaphore;

    // Read/Write Parameters
    static int ports[] = {8001, 8002, 8003, 8004, 8005, 8006, 8007, 8008, 8009, 8010};
    static int THREADS;
    static int HEADER_LENGTH;
    static long SHARES_FILE_SIZE;
    static long VERIFICATION_FILE_SIZE;
    public static String FILE_PATH;
    static int MAX_BUFFER_SIZE = 1024 * 1024 * 1;

    static void initializeParameters(int MOD_SIZE, long SHARE_FILE_SIZE, boolean verifiability) {
        Parameters.VERIFIABILITY = verifiability;
        Parameters.THREADS = Runtime.getRuntime().availableProcessors();
        Parameters.MOD_SIZE = MOD_SIZE;
        Parameters.MOD_LENGTH = MOD_SIZE*8;
        Parameters.SHARE_SIZE = verifiability ? 3*(MOD_SIZE) : MOD_SIZE;
        Parameters.SHARES_FILE_SIZE = SHARE_FILE_SIZE;


        VERIFICATION_FILE_SIZE = 3* SHARES_FILE_SIZE;
        BUFFER_SIZE = (int) Math.min( Math.ceil(SHARE_FILE_SIZE / THREADS) , MAX_BUFFER_SIZE);
        BUFFER_SIZE = BUFFER_SIZE - BUFFER_SIZE % SHARE_SIZE;



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