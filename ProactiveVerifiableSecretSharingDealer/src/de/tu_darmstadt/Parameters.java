package de.tu_darmstadt;

import com.j256.ormlite.dao.Dao;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.Semaphore;

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
    public static int BUFFER_SIZE;
    public static String FILE_PATH;
    public static int MOD_SIZE;
    public static boolean VERIFIABILITY;
    static short BITS; // in bits, must be power of 2
    static int MOD_LENGTH; // in bits, must be power of 2 or = 0 mod 8

    // Read/Write Parameters
    public static Dao<ShareHolder, String> shareholdersDao;
    public static Dao<StoredFile, String> storedFileDao;
    public static Dao<ManyToMany, String> manyToManyDao;
    public static Semaphore dbSemaphore;
    static ShareHolder[] shareHolders;
    static SSLClient[] sslClients;
    public static int THREADS;
    public static long SHARES_FILE_SIZE;
    public static int MAX_BUFFER_SIZE = 1024 * 1024 * 1; // 100 for Encryption

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

    public static boolean readProperties(String name){
        try{
            Properties prop = new Properties();
            FileInputStream fin = new FileInputStream(name);
            prop.load(fin);
            String propertyValue = prop.getProperty("shareholders");
            if (propertyValue != null){
                SHAREHOLDERS = Integer.parseInt(propertyValue);
                if (SHAREHOLDERS <= 0 || SHAREHOLDERS>25) return false;
            }else return false;

            propertyValue = prop.getProperty("needed_shares");
            if (propertyValue != null){
                NEEDED_SHARES = Integer.parseInt(propertyValue);
                if (NEEDED_SHARES <= 0 || NEEDED_SHARES>SHAREHOLDERS) return false;
            }else return false;

            propertyValue = prop.getProperty("file_path");
            if (propertyValue != null){
                FILE_PATH  = propertyValue;
                if (!new File(FILE_PATH).isFile()) return false;
            }else return false;

            propertyValue = prop.getProperty("file_size");
            if (propertyValue != null){
                TARGET_FILE_SIZE = Long.parseLong(propertyValue);
                if (TARGET_FILE_SIZE <= 0) return false;
            }else return false;

            propertyValue = prop.getProperty("bits");
            if (propertyValue != null){
                BITS = Short.parseShort(propertyValue);
                if (BITS <= 0 || BITS % 2 != 0) return false;
            }else return false;

            propertyValue = prop.getProperty("modulus");
            if (propertyValue != null){
                MODULUS = new BigInteger(propertyValue);
            }else return false;

            fin.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static boolean writeProperties(String name){
        try{
            Properties prop = new Properties();
            FileOutputStream fout = new FileOutputStream(name);
            prop.setProperty("shareholders", String.valueOf(SHAREHOLDERS));
            prop.setProperty("needed_shares", String.valueOf(NEEDED_SHARES));
            prop.setProperty("file_path", FILE_PATH);
            prop.setProperty("file_size", String.valueOf(TARGET_FILE_SIZE));
            prop.setProperty("bits", String.valueOf(BITS));
            prop.setProperty("modulus", MODULUS.toString());
            prop.store(fout, null);

            fout.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }


}
