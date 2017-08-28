package de.tu_darmstadt;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


/**
 * Created by Stathis on 6/11/17.
 */

public class VerificationTask implements Callable {
    private BigInteger[] buffer;
    private int xValue;
    private long destStartingByte;
    private  String fileName;
    private int neededShares;

    public VerificationTask(int neededShares, String fileName, int xValue, BigInteger[] buffer, long destStartingByte) {
        this.buffer = buffer;
        this.xValue = xValue;
        this.destStartingByte = destStartingByte;
        this.fileName = fileName;
        this.neededShares = neededShares;
    }

    @Override
    public Integer call() {

        try {
            RandomAccessFile shareFile = new RandomAccessFile(SHARE_DIR + fileName, "rw");
            shareFile.seek(destStartingByte);

            for (int i = 0; i< buffer.length; i= i + (neededShares+2)) {

                BigInteger shareIndex = BigInteger.valueOf(xValue);
                BigInteger share = buffer[i];
                BigInteger shareCommitment = buffer[i+1];
                BigInteger[] publicCommitments = Arrays.copyOfRange(buffer, i+2, i+2+neededShares);

                boolean status = BigIntegerPolynomial.verifyCommitment(shareIndex, share, shareCommitment, publicCommitments, pedersenParameters.getP());

                if (!status) throw new Exception("Not Valid Share detected");
                shareFile.write(fixLength(share.toByteArray(), Parameters.SHARE_SIZE));

            }
            show("verification success");

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}