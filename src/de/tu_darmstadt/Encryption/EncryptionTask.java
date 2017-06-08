package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;

import static de.tu_darmstadt.Parameters.*;


public class EncryptionTask implements Runnable {

    private long sourceStartingByte;
    private long sourceEndingByte;
    private long destStartingByte;

    public EncryptionTask(long sourceStartingByte, long sourceEndingByte, long destStartingByte) {
        this.sourceStartingByte = sourceStartingByte;
        this.sourceEndingByte = sourceEndingByte;
        this.destStartingByte = destStartingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILE_PATH, "r");
            RandomAccessFile targetFiles[] = new RandomAccessFile[SHAREHOLDERS];
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;

            sourceFile.seek(sourceStartingByte);

            for (int j = 0; j < SHAREHOLDERS; j++) {
                targetFiles[j] = new RandomAccessFile(FILE_PATH + j, "rw");
                targetFiles[j].seek(destStartingByte);
            }

            while (processed < contentLength) {
                byte buffer[];
                if (contentLength - processed >= BUFFER_SIZE) {
                    buffer = new byte[BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (contentLength - processed)];

                }

                sourceFile.readFully(buffer);
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
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    targetFiles[j].write(encryptedData[j]);
                }
                processed += buffer.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
