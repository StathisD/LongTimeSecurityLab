package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


/**
 * Created by Stathis on 6/11/17.
 */

public class VerificationTask implements Callable {
    private byte[] buffer;
    private int xValue;


    public VerificationTask(byte[] buffer, int xValue) {
        this.buffer = buffer;
        this.xValue = xValue;


    }

    @Override
    public Integer call() {

        try {
            int numbersInBuffer = buffer.length / SHARE_SIZE;

            for (int i = 0; i<numbersInBuffer; i++) {
                // reconstruct number

                byte[] oneNumber = Arrays.copyOfRange(buffer, i * SHARE_SIZE, (i+1) * SHARE_SIZE);

                byte[] bytes = Arrays.copyOfRange(oneNumber,0, MOD_SIZE);
                BigInteger publicCommitment = new BigInteger(1, bytes);
                bytes = Arrays.copyOfRange(oneNumber, MOD_SIZE, 2*MOD_SIZE);
                BigInteger shareCommitment = new BigInteger(1, bytes);
                bytes = Arrays.copyOfRange(oneNumber, 2*MOD_SIZE, 3*MOD_SIZE);
                BigInteger share = new BigInteger(1, bytes);

                //boolean status = BigIntegerPolynomial.verifyCommitment(BigInteger.valueOf(xValue), share, shareCommitment, publicCommitment, MODULUS);

                //if (!status) throw new Exception("Not Valid Share detected");

            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}