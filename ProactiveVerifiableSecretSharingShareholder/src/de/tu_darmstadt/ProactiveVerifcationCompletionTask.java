package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.pedersenParameters;
import static de.tu_darmstadt.Parameters.show;

/**
 * Created by Stathis on 8/16/17.
 */
public class ProactiveVerifcationCompletionTask implements Callable<BigInteger[]> {
    private BigInteger[] buffer;
    private int xValue;
    private int neededShares;

    public ProactiveVerifcationCompletionTask(int neededShares, int xValue, BigInteger[] buffer) {
        this.buffer = buffer;
        this.xValue = xValue;
        this.neededShares = neededShares;
    }

    public BigInteger[] call() {

        try {

            BigInteger[] numbers = new BigInteger[buffer.length / (neededShares + 2)];
            int j = 0;
            for (int i = 0; i < buffer.length; i = i + (neededShares + 2)) {

                BigInteger shareIndex = BigInteger.valueOf(xValue);
                BigInteger share = buffer[i];
                BigInteger shareCommitment = buffer[i + 1];
                BigInteger[] publicCommitments = Arrays.copyOfRange(buffer, i + 2, i + 2 + neededShares);

                boolean status = BigIntegerPolynomial.verifyCommitment(shareIndex, share, shareCommitment, publicCommitments, pedersenParameters.getP());

                if (!status) {
                    show(i);
                    throw new Exception("Not Valid Share detected");
                }
                numbers[j] = share;
                j++;

            }
            show("verification success");

            return numbers;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
