package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


public class EncryptionTask implements Callable<BigInteger[][]> {

    private byte[] buffer;

    public EncryptionTask(byte[] buffer) {
        this.buffer = buffer;
    }

    @Override
    public BigInteger[][] call() {
        int numbersInBuffer = (int) Math.ceil(buffer.length * 1.0 / BLOCK_SIZE);

        BigInteger[][] encryptedNumbers;

        if (VERIFIABILITY) {
            encryptedNumbers = new BigInteger[SHAREHOLDERS][numbersInBuffer * (NEEDED_SHARES + 2)];
        } else {
            encryptedNumbers = new BigInteger[SHAREHOLDERS][numbersInBuffer];
        }


        int currentPos = 0;
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

            //byte[] byteShare = new byte[SHARE_SIZE];


            for (int x = 0; x < SHAREHOLDERS; x++) {

                List<BigInteger> list = new ArrayList<BigInteger>();

                BigInteger xValue = BigInteger.valueOf(x + 1);
                BigInteger yValue = polynomial.evaluatePolynom(xValue);
                list.add(yValue);
                if (VERIFIABILITY) {
                    BigInteger shareCommitment = polynomial.G.evaluatePolynom(xValue);
                    list.add(shareCommitment);
                    list.addAll(Arrays.asList(polynomial.commitments));
                }
                BigInteger[] share = list.toArray(new BigInteger[list.size()]);
                System.arraycopy(share, 0, encryptedNumbers[x], currentPos, share.length);
                if (x == SHAREHOLDERS - 1) currentPos += share.length;
            }
        }

        return encryptedNumbers;
    }
}
