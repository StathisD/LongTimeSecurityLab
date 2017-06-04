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

    private long startingByte;
    private long endingByte;

    public EncryptionTask(long startingByte, long endingByte) {
        this.startingByte = startingByte;
        this.endingByte = endingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile sourceFile = new RandomAccessFile(FILEPATH, "r");
            RandomAccessFile targetFiles[] = new RandomAccessFile[SHAREHOLDERS];
            long copied = 0;
            long contentLength = endingByte - startingByte;

            sourceFile.seek(startingByte);
            for (int j = 0; j < SHAREHOLDERS; j++) {
                targetFiles[j] = new RandomAccessFile(FILEPATH + j, "rw");
                targetFiles[j].seek(startingByte + 18 + MODSIZE);
            }

            while (copied < contentLength) {
                byte buffer[];
                if (contentLength - copied > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (contentLength - copied)];
                }
                sourceFile.readFully(buffer);

                int encodedSize = (int) Math.ceil(buffer.length * 1.0 / BLOCKSIZE);
                byte[][] encryptedData = new byte[SHAREHOLDERS][encodedSize * SHARESIZE];
                byte[] oneNumber;

                for (int i = 0; i < encodedSize; i++) {
                    if ((i + 1) * BLOCKSIZE <= buffer.length) {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCKSIZE, (i + 1) * BLOCKSIZE);
                    } else {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCKSIZE, buffer.length);
                    }

                    BigInteger number = new BigInteger(1, oneNumber);

                    //encrypt
                    BigIntegerPolynomial polynomial = new BigIntegerPolynomial(SHAREHOLDERS - 1, MODULUS, number);

                    byte[] byteShare;

                    for (int x = 0; x < SHAREHOLDERS; x++) {
                        BigInteger xValue = BigInteger.valueOf(x + 1);
                        BigInteger yValue = polynomial.evaluate(xValue);
                        byte[] xValueBytes = xValue.toByteArray();
                        byte[] yValueBytes = fixLength(yValue.toByteArray(), MODSIZE);
                        byteShare = concat(xValueBytes, yValueBytes);
                        System.arraycopy(byteShare, 0, encryptedData[x], i * SHARESIZE, byteShare.length);
                    }
                }


                for (int j = 0; j < SHAREHOLDERS; j++) {

                    targetFiles[j].write(encryptedData[j]);
                }
                copied += buffer.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
