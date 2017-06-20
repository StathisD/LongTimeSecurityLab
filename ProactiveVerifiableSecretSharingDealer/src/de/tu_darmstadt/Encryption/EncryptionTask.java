package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import javax.xml.bind.DatatypeConverter;
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
                    byte[] bytes;
                    for (int j = 0; j < polynomial.commitments.length; j++) {
                        bytes = fixLength(polynomial.commitments[j].toByteArray(), MOD_SIZE);
                        System.arraycopy(bytes, 0, byteShare, j*MOD_SIZE, MOD_SIZE);
                    }
                    BigInteger shareCommitment = polynomial.G.evaluatePolynom(xValue);
                    bytes = fixLength(shareCommitment.toByteArray(), MOD_SIZE);
                    System.arraycopy(bytes, 0, byteShare, (NEEDED_SHARES )* MOD_SIZE, MOD_SIZE);
                    bytes = fixLength(yValue.toByteArray(), MOD_SIZE);
                    System.arraycopy(bytes, 0, byteShare, (NEEDED_SHARES + 1) * MOD_SIZE, MOD_SIZE);

                    //Check
                    BigInteger[] publicCommitments = new BigInteger[NEEDED_SHARES];
                    for (int j = 0; j < NEEDED_SHARES; j++) {
                        bytes = Arrays.copyOfRange(byteShare,j*MOD_SIZE, (j+1)*MOD_SIZE);
                        publicCommitments[j] = new BigInteger(1, bytes);
                    }

                    bytes = Arrays.copyOfRange(byteShare, (NEEDED_SHARES)*MOD_SIZE, (NEEDED_SHARES + 1)*MOD_SIZE);
                    shareCommitment = new BigInteger(1, bytes);
                    bytes = Arrays.copyOfRange(byteShare, (NEEDED_SHARES + 1)*MOD_SIZE, (NEEDED_SHARES + 2)*MOD_SIZE);
                    BigInteger share = new BigInteger(1, bytes);

                    boolean status = BigIntegerPolynomial.verifyCommitment(xValue, share, shareCommitment, publicCommitments, MODULUS);
                    show(status);
                } else {
                    byteShare = fixLength(yValue.toByteArray(), SHARE_SIZE);
                }

                System.arraycopy(byteShare, 0, encryptedData[x], i * SHARE_SIZE, byteShare.length);
            }

        }

        return encryptedData;
    }
}
