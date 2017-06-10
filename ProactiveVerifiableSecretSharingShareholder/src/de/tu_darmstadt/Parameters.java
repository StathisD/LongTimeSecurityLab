package de.tu_darmstadt;

import java.math.BigInteger;


/**
 * Created by stathis on 6/3/17.
 */
public final class Parameters {

    // Encryption Parameters
    public static BigInteger MODULUS;

    public static long SHARES_FILE_SIZE_WITH_HEADER;
    public static String FILE_PATH;

    // Read/Write Parameters
    static int ports[] = {8001, 8002, 8003, 8004, 8005, 8006, 8007, 8008, 8009, 8010};
    static int THREADS;
    static int HEADER_LENGTH;
    static long SHARES_FILE_SIZE_WITHOUT_HEADER;

    static void initializeParameters(int HEADER, long SHARE_FILE_SIZE) {
        Parameters.THREADS = Runtime.getRuntime().availableProcessors();
        Parameters.HEADER_LENGTH = HEADER;

        Parameters.SHARES_FILE_SIZE_WITHOUT_HEADER = SHARE_FILE_SIZE;
        Parameters.SHARES_FILE_SIZE_WITH_HEADER = SHARE_FILE_SIZE + HEADER_LENGTH;

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
