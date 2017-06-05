package de.tu_darmstadt.Decryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import javax.xml.bind.DatatypeConverter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;

import static de.tu_darmstadt.Parameters.*;


public class DecryptionTask implements Runnable {

    private long sourceStartingByte;
    private long sourceEndingByte;
    private long destStartingByte;
    private long destEndingByte;

    public DecryptionTask(long sourceStartingByte, long sourceEndingByte, long destStartingByte, long destEndingByte) {
        this.sourceStartingByte = sourceStartingByte;
        this.sourceEndingByte = sourceEndingByte;
        this.destStartingByte = destStartingByte;
        this.destEndingByte = destEndingByte;
    }

    @Override
    public void run() {
        try {
            RandomAccessFile[] sourceFiles = new RandomAccessFile[SHAREHOLDERS];
            RandomAccessFile targetFile = new RandomAccessFile(FILEPATH + "_dec", "rw");
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;

            boolean lastChunk = false;
            boolean lastBuffer = false;
            int sizeLastNumber = 0;
            if (sourceEndingByte == SHARES_FILE_SIZE) {
                lastChunk = true;
                sizeLastNumber = (int) (TARGET_FILE_SIZE % BLOCKSIZE);
            }

            targetFile.seek(destStartingByte);

            for (int j = 0; j < SHAREHOLDERS; j++) {
                sourceFiles[j] = new RandomAccessFile(FILEPATH + j, "r");
                sourceFiles[j].seek(sourceStartingByte);
            }
            long bytesProcessed = targetFile.getFilePointer();

            while (processed < contentLength) {
                byte[][] buffer;
                int encodedSize;
                byte[][] oneNumber = new byte[SHAREHOLDERS][];
                byte[] outBuffer;
                BigInteger[][] shares = new BigInteger[SHAREHOLDERS][2];

                if (contentLength - processed >= SHARES_BUFFER_SIZE) {
                    buffer = new byte[SHAREHOLDERS][SHARES_BUFFER_SIZE];
                    encodedSize = SHARES_BUFFER_SIZE / SHARESIZE;
                    outBuffer = new byte[encodedSize * BLOCKSIZE];
                } else {
                    // last chunk, last buffer
                    buffer = new byte[SHAREHOLDERS][(int) (contentLength - processed)];
                    encodedSize = (int) Math.ceil(buffer[0].length * 1.0 / SHARESIZE);
                    outBuffer = new byte[(encodedSize - 1) * BLOCKSIZE + sizeLastNumber];
                    lastBuffer = true;

                }
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    sourceFiles[j].readFully(buffer[j]);
                }
                boolean guard = false;
                for (int i = 0; i < encodedSize; i++) {
                    for (int j = 0; j < SHAREHOLDERS; j++) {
                        if ((i + 1) * SHARESIZE <= buffer[j].length) {
                            oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARESIZE, (i + 1) * SHARESIZE);
                        } else {
                            guard = true;
                            oneNumber[j] = Arrays.copyOfRange(buffer[j], i * SHARESIZE, buffer[j].length);
                        }

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
                    BigInteger decryptedNumber;
                    if (guard) {
                        decryptedNumber = BigIntegerPolynomial.interpolate(shares, SHAREHOLDERS, BigInteger.ZERO, MODULUS);
                    } else {
                        decryptedNumber = BigInteger.ZERO;
                    }
                    byte[] decryptedBytes = decryptedNumber.toByteArray();

                    if (guard) {
                        show(contentLength % SHARESIZE);
                        System.out.println(DatatypeConverter.printHexBinary(decryptedBytes));
                        decryptedBytes = fixLength(decryptedBytes, sizeLastNumber);
                        show(bytesProcessed);
                        System.out.println(DatatypeConverter.printHexBinary(decryptedBytes));
                        System.out.println(sourceFiles[0].getFilePointer());
                        System.out.println(targetFile.getFilePointer());
                    } else {

                        decryptedBytes = fixLength(decryptedBytes, BLOCKSIZE);

                    }
                    bytesProcessed += decryptedBytes.length;
                    System.arraycopy(decryptedBytes, 0, outBuffer, i * BLOCKSIZE, decryptedBytes.length);
                }
                targetFile.write(outBuffer);
                //System.out.println("Space: " + (destEndingByte -destStartingByte));
                //System.out.println("Used: " + outBuffer.length);
                //System.out.println();
                processed += buffer[0].length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
