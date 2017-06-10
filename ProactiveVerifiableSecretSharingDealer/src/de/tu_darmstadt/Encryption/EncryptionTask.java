package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


public class EncryptionTask implements Callable<byte[][]> {


    private byte[] buffer;

    public EncryptionTask(byte[] buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte[][] call() {
        //show("Buffer received:" + buffer.length);
        int numbersInBuffer = (int) Math.ceil(buffer.length * 1.0 / BLOCK_SIZE);
        byte[][] encryptedData = new byte[SHAREHOLDERS][numbersInBuffer * SHARE_SIZE];
        byte[] oneNumber;

        for (int i = 0; i < numbersInBuffer; i++) {
            if ((i + 1) * BLOCK_SIZE <= buffer.length) {
                oneNumber = Arrays.copyOfRange(buffer, i * BLOCK_SIZE, (i + 1) * BLOCK_SIZE);
            } else {
                oneNumber = Arrays.copyOfRange(buffer, i * BLOCK_SIZE, buffer.length);
            }

            BigInteger number = new BigInteger(1, oneNumber);

            //encrypt
            BigIntegerPolynomial polynomial = new BigIntegerPolynomial(NEEDED_SHARES - 1, MODULUS, number);

            byte[] byteShare;

            for (int x = 0; x < SHAREHOLDERS; x++) {
                BigInteger xValue = BigInteger.valueOf(x + 1);
                BigInteger yValue = polynomial.evaluate(xValue);
                byteShare = fixLength(yValue.toByteArray(), SHARE_SIZE);
                System.arraycopy(byteShare, 0, encryptedData[x], i * SHARE_SIZE, byteShare.length);
            }
        }
        //show("Buffer giving: " +encryptedData[0].length);
        return encryptedData;
    }
}
