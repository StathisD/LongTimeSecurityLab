package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;

/**
 * Created by Stathis on 8/16/17.
 */
public class ProactiveVerificationInitializationTask implements Callable {
    private int numbersInBuffer;
    private Share share;
    private List<ShareHolder> shareHolderList;

    public ProactiveVerificationInitializationTask(int numbersInBuffer, Share share, List<ShareHolder> shareHolderList) {
        this.numbersInBuffer = numbersInBuffer;
        this.share = share;
        this.shareHolderList = shareHolderList;
    }

    public BigInteger[][] call() {
        try {
            BigInteger[][] remoteNumbers;
            if (VERIFIABILITY) {
                remoteNumbers = new BigInteger[shareHolderList.size()][numbersInBuffer * (share.getNeededShares() + 2)];
            } else {
                remoteNumbers = new BigInteger[shareHolderList.size()][numbersInBuffer];
            }

            // for each number in buffer calculate the new numbers for the shareholders
            for (int k = 0; k < numbersInBuffer; k++) {
                BigIntegerPolynomial currentPolynomial = new BigIntegerPolynomial(share.getNeededShares() - 1, MODULUS, BigInteger.ZERO);
                int j = 0;
                for (ShareHolder shareholder : shareHolderList) {
                    List<BigInteger> list = new ArrayList<BigInteger>();
                    BigInteger yValue = currentPolynomial.evaluatePolynom(shareholder.getxValue());
                    list.add(yValue);

                    // add verifiability
                    if (VERIFIABILITY && !shareholder.getName().equals(SERVER_NAME)) {
                        BigInteger shareCommitment = currentPolynomial.G.evaluatePolynom(shareholder.getxValue());
                        list.add(shareCommitment);
                        list.addAll(Arrays.asList(currentPolynomial.commitments));
                    }
                    BigInteger[] shareArray = list.toArray(new BigInteger[list.size()]);
                    System.arraycopy(shareArray, 0, remoteNumbers[j], k * shareArray.length, shareArray.length);
                    j++;
                }
            }
            return remoteNumbers;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
