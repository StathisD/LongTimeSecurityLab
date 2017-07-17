package de.tu_darmstadt;

import com.j256.ormlite.dao.Dao;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
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
    public static int MOD_SIZE;
    public static boolean VERIFIABILITY;
    public static int BLOCK_SIZE; // in Bytes
    public static long TARGET_FILE_SIZE;
    static short BITS; // in bits, must be power of 2
    static int MOD_LENGTH; // in bits, must be power of 2 or = 0 mod 8

    public static long timeSlot = 1000*10;


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

    static void initializeParameters(short BITS, long TARGET_FILE_SIZE, int mode, boolean verifiability) {
        Parameters.VERIFIABILITY = verifiability;
        Parameters.BITS = BITS;
        Parameters.BLOCK_SIZE = BITS / 8;
        Parameters.MOD_LENGTH = BITS + 8;
        Parameters.MOD_SIZE = MOD_LENGTH / 8;
        Parameters.SHARE_SIZE = verifiability ? (NEEDED_SHARES + 2) * (MOD_SIZE) : MOD_SIZE;
        Parameters.THREADS = Runtime.getRuntime().availableProcessors();
        Parameters.TARGET_FILE_SIZE = TARGET_FILE_SIZE;
        long x = (long) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCK_SIZE);
        Parameters.SHARES_FILE_SIZE = x * SHARE_SIZE;

        int numberSize;
        long fileSize;
        if (mode == 0) {
            byte[] bytes = new byte[BITS / 8];
            Arrays.fill(bytes, (byte) 0xff);
            BigInteger number = new BigInteger(1, bytes);
            MODULUS = new BigInteger(MOD_LENGTH, 100000, new Random());
            while (number.compareTo(MODULUS) > 0) {
                MODULUS = new BigInteger(MOD_LENGTH, 100000, new Random());
            }
            numberSize = BLOCK_SIZE;
            fileSize = TARGET_FILE_SIZE;
        } else {
            numberSize = SHARE_SIZE;
            fileSize = SHARES_FILE_SIZE;
        }

        BUFFER_SIZE = (int) Math.min(Math.ceil(fileSize / THREADS), MAX_BUFFER_SIZE);
        BUFFER_SIZE = BUFFER_SIZE - BUFFER_SIZE % numberSize;

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
