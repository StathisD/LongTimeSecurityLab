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
            RandomAccessFile[] sourceFiles = new RandomAccessFile[SHAREHOLDERS];
            RandomAccessFile targetFile = new RandomAccessFile(FILEPATH + "_dec", "rw");
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;

            boolean lastBuffer = false;
            int sizeLastNumber = 0;
            if (sourceEndingByte == SHARES_FILE_SIZE_WITH_HEADER) {
                //lastChunk = true;
                sizeLastNumber = (int) (TARGET_FILE_SIZE % BLOCKSIZE);


            }

            targetFile.seek(destStartingByte);

            for (int j = 0; j < SHAREHOLDERS; j++) {
                sourceFiles[j] = new RandomAccessFile(FILEPATH + j, "r");
                sourceFiles[j].seek(sourceStartingByte);
            }

            while (processed < contentLength) {
                byte[][] buffer;
                int numbersInBuffer;
                byte[][] oneNumber = new byte[SHAREHOLDERS][];
                byte[] outBuffer;
                BigInteger[][] shares = new BigInteger[SHAREHOLDERS][2];

                if (contentLength - processed >= SHARES_BUFFER_SIZE) {
                    buffer = new byte[SHAREHOLDERS][SHARES_BUFFER_SIZE];
                    numbersInBuffer = SHARES_BUFFER_SIZE / SHARESIZE;
                    outBuffer = new byte[numbersInBuffer * BLOCKSIZE];
                } else {
                    // last chunk, last buffer
                    buffer = new byte[SHAREHOLDERS][(int) (contentLength - processed)];
                    numbersInBuffer = buffer[0].length / SHARESIZE;
                    outBuffer = new byte[(numbersInBuffer - 1) * BLOCKSIZE + sizeLastNumber];
                    lastBuffer = true;

                }
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    sourceFiles[j].readFully(buffer[j]);
                }
                for (int i = 0; i < numbersInBuffer; i++) {

                    // reconstruct number
                    for (int j = 0; j < SHAREHOLDERS; j++) {

                        oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARESIZE, (i + 1) * SHARESIZE);

                        byte[] xValueBytes = new byte[1];
                        byte[] yValueBytes = new byte[MODSIZE];
                        xValueBytes[0] = oneNumber[j][0];
                        System.arraycopy(oneNumber[j], 1, yValueBytes, 0, oneNumber[j].length - 1);
                        BigInteger xValue = new BigInteger(1, xValueBytes);
                        BigInteger yValue = new BigInteger(1, yValueBytes);
                        shares[j][0] = xValue;
                        shares[j][1] = yValue;
                    }

                    //decrypt
                    BigInteger decryptedNumber = BigIntegerPolynomial.interpolate(shares, SHAREHOLDERS, BigInteger.ZERO, MODULUS);
                    byte[] decryptedBytes = decryptedNumber.toByteArray();

                    if (i == numbersInBuffer - 1 && lastBuffer) {
                        decryptedBytes = fixLength(decryptedBytes, sizeLastNumber);
                    } else {
                        decryptedBytes = fixLength(decryptedBytes, BLOCKSIZE);
                    }

                    System.arraycopy(decryptedBytes, 0, outBuffer, i * BLOCKSIZE, decryptedBytes.length);
                }
                targetFile.write(outBuffer);
                processed += buffer[0].length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
