package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

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

            byte[] byteShare = new byte[SHARE_SIZE];

            for (int x = 0; x < SHAREHOLDERS; x++) {
                BigInteger xValue = BigInteger.valueOf(x + 1);
                BigInteger yValue = polynomial.evaluatePolynom(xValue);

                if (VERIFIABILITY) {

                    BigInteger shareCommitment = polynomial.G.evaluatePolynom(xValue);

                    byte[] bytes = fixLength(polynomial.commitment.toByteArray(), MOD_SIZE);
                    System.arraycopy(bytes, 0, byteShare, 0, MOD_SIZE);
                    bytes = fixLength(shareCommitment.toByteArray(), MOD_SIZE);
                    System.arraycopy(bytes, 0, byteShare, MOD_SIZE, MOD_SIZE);
                    bytes = fixLength(yValue.toByteArray(), MOD_SIZE);
                    System.arraycopy(bytes, 0, byteShare, 2 * MOD_SIZE, MOD_SIZE);
                    //show(i);
                } else {
                    byteShare = fixLength(yValue.toByteArray(), SHARE_SIZE);
                }

                System.arraycopy(byteShare, 0, encryptedData[x], i * SHARE_SIZE, byteShare.length);
            }
        }
        return encryptedData;
    }
}
