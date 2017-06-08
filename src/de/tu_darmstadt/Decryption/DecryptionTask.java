package de.tu_darmstadt.Decryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;

import static de.tu_darmstadt.Parameters.*;


public class DecryptionTask implements Runnable {

    private long sourceStartingByte;
    private long sourceEndingByte;
    private long destStartingByte;


    public DecryptionTask(long sourceStartingByte, long sourceEndingByte, long destStartingByte) {
        this.sourceStartingByte = sourceStartingByte;
        this.sourceEndingByte = sourceEndingByte;
        this.destStartingByte = destStartingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile[] sourceFiles = new RandomAccessFile[NEEDED_SHARES];
            RandomAccessFile targetFile = new RandomAccessFile(FILE_PATH + "_dec", "rw");
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;
            boolean lastBuffer = false;
            int sizeLastNumber = 0;
            if (sourceEndingByte == SHARES_FILE_SIZE_WITH_HEADER) {
                sizeLastNumber = (int) (TARGET_FILE_SIZE % BLOCK_SIZE);

            }

            targetFile.seek(destStartingByte);

            for (int j = 0; j < NEEDED_SHARES; j++) {
                sourceFiles[j] = new RandomAccessFile(FILE_PATH + j, "r");
                sourceFiles[j].seek(sourceStartingByte);
            }

            while (processed < contentLength) {
                byte[][] buffer;
                int numbersInBuffer;
                byte[][] oneNumber = new byte[NEEDED_SHARES][];
                byte[] outBuffer;
                BigInteger[] shares = new BigInteger[NEEDED_SHARES];

                if (contentLength - processed >= BUFFER_SIZE) {

                    buffer = new byte[NEEDED_SHARES][BUFFER_SIZE];
                    numbersInBuffer = BUFFER_SIZE / SHARE_SIZE;
                    outBuffer = new byte[numbersInBuffer * BLOCK_SIZE];

                } else {
                    // last chunk, last buffer
                    buffer = new byte[NEEDED_SHARES][(int) (contentLength - processed)];
                    numbersInBuffer = buffer[0].length / SHARE_SIZE;
                    outBuffer = new byte[(numbersInBuffer - 1) * BLOCK_SIZE + sizeLastNumber];
                    lastBuffer = true;
                }
                for (int j = 0; j < NEEDED_SHARES; j++) {
                    sourceFiles[j].readFully(buffer[j]);
                }
                processed += buffer[0].length;

                for (int i = 0; i < numbersInBuffer; i++) {

                    // reconstruct number
                    for (int j = 0; j < NEEDED_SHARES; j++) {

                        oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARE_SIZE, (i + 1) * SHARE_SIZE);
                        BigInteger yValue = new BigInteger(1, oneNumber[j]);
                        shares[j] = yValue;
                    }

                    //decrypt
                    BigInteger decryptedNumber = BigIntegerPolynomial.interpolate(shares, MODULUS);
                    byte[] decryptedBytes = decryptedNumber.toByteArray();

                    if (i == numbersInBuffer - 1 && lastBuffer) {
                        decryptedBytes = fixLength(decryptedBytes, sizeLastNumber);
                    } else {
                        decryptedBytes = fixLength(decryptedBytes, BLOCK_SIZE);
                    }

                    System.arraycopy(decryptedBytes, 0, outBuffer, i * BLOCK_SIZE, decryptedBytes.length);
                }
                targetFile.write(outBuffer);
            }
            show("End");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
