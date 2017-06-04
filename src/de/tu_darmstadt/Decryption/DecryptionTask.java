package de.tu_darmstadt.Decryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.TreeMap;

import static de.tu_darmstadt.Parameters.*;


public class DecryptionTask implements Runnable {

    private long startingByte;
    private long endingByte;

    public DecryptionTask(long startingByte, long endingByte) {
        this.startingByte = startingByte;
        this.endingByte = endingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile[] sourceFiles = new RandomAccessFile[SHAREHOLDERS];
            RandomAccessFile targetFile = new RandomAccessFile(FILEPATH + "_dec", "rw");
            long copied = 0;
            long contentLength = endingByte - startingByte;

            targetFile.seek(startingByte);
            for (int j = 0; j < SHAREHOLDERS; j++) {
                sourceFiles[j] = new RandomAccessFile(FILEPATH + j, "r");
                sourceFiles[j].seek(startingByte + 18 + MODSIZE);
            }

            while (copied < contentLength) {
                byte[][] buffer = new byte[SHAREHOLDERS][];
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    if (contentLength - copied > SHARESIZE) {
                        buffer[j] = new byte[SHARESIZE];
                    } else {
                        buffer[j] = new byte[(int) (contentLength - copied)];
                    }
                    sourceFiles[j].readFully(buffer[j]);
                }

                int encodedSize = (int) Math.ceil(buffer[0].length * 1.0 / SHARESIZE);
                byte[][] oneNumber = new byte[SHAREHOLDERS][];
                byte[] outBuffer = new byte[encodedSize * BLOCKSIZE];

                for (int i = 0; i < encodedSize; i++) {
                    TreeMap<BigInteger, BigInteger> shares = new TreeMap<>();
                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        if ((i + 1) * SHARESIZE <= buffer[j].length) {
                            oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARESIZE, (i + 1) * SHARESIZE);
                        } else {
                            oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARESIZE, buffer[0].length);
                        }

                        byte[] xValueBytes = new byte[1];
                        byte[] yValueBytes = new byte[MODSIZE];
                        xValueBytes[0] = oneNumber[j][0];
                        System.arraycopy(oneNumber[j], 1, yValueBytes, 0, oneNumber[j].length - 1);
                        BigInteger xValue = new BigInteger(1, xValueBytes);
                        BigInteger yValue = new BigInteger(1, yValueBytes);
                        shares.put(xValue, yValue);
                    }
                    //decrypt
                    BigInteger decryptedNumber = BigIntegerPolynomial.interpolate(shares, SHAREHOLDERS, BigInteger.ZERO, MODULUS);
                    byte[] decryptedBytes = decryptedNumber.toByteArray();
                    decryptedBytes = fixLength(decryptedBytes, BLOCKSIZE);

                    System.arraycopy(decryptedBytes, 0, outBuffer, i * BLOCKSIZE, BLOCKSIZE);

                }
                targetFile.write(outBuffer);

                copied += buffer[0].length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
