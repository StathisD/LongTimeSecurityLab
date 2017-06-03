package de.tu_darmstadt.Encryption;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by stathis on 5/31/17.
 */
public class EncryptionTask implements Callable<byte[]> {
    private int bits;
    private int blockLength;
    private byte[] data;

    public EncryptionTask(byte[] data, int bits) {
        this.data = data;
        this.bits = bits;
        this.blockLength = bits / 8;
    }

    public byte[] call() {

        int encodedSize = (int) Math.ceil(data.length * 1.0 / blockLength);
        byte[] encryptedData = new byte[data.length];
        byte[] oneNumber;

        for (int i = 0; i < encodedSize; i++) {
            if ((i + 1) * blockLength <= data.length) {
                oneNumber = Arrays.copyOfRange(data, i * blockLength, (i + 1) * blockLength);
            } else {
                oneNumber = Arrays.copyOfRange(data, i * blockLength, data.length);

            }
            BigInteger number = new BigInteger(oneNumber);
            //encrypt
            BigInteger encryptedNumber = number;

            byte[] encNumber = encryptedNumber.toByteArray();

            //add Padding if needed
            if (encNumber.length < oneNumber.length) {
                int sign = encryptedNumber.signum();
                byte[] newData = new byte[oneNumber.length];
                if (sign == -1) Arrays.fill(newData, (byte) 0xff);
                System.arraycopy(encNumber, 0, newData, (newData.length - encNumber.length), encNumber.length);
                encNumber = newData;
            }

            System.arraycopy(encNumber, 0, encryptedData, i * blockLength, encNumber.length);
        }

        return encryptedData;
    }
}


