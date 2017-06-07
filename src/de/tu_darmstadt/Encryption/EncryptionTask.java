package de.tu_darmstadt.Encryption;

/**
 * Created by stathis on 6/4/17.
 */
import de.tu_darmstadt.BigIntegerPolynomial;

import javax.xml.bind.DatatypeConverter;
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
            RandomAccessFile sourceFile = new RandomAccessFile(FILEPATH, "r");
            RandomAccessFile targetFiles[] = new RandomAccessFile[SHAREHOLDERS];
            long processed = 0;
            long contentLength = sourceEndingByte - sourceStartingByte;

            sourceFile.seek(sourceStartingByte);

            for (int j = 0; j < SHAREHOLDERS; j++) {
                targetFiles[j] = new RandomAccessFile(FILEPATH + j, "rw");
                targetFiles[j].seek(destStartingByte);
            }

            while (processed < contentLength) {
                byte buffer[];
                if (contentLength - processed >= TARGET_BUFFER_SIZE) {
                    buffer = new byte[TARGET_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (contentLength - processed)];
                }

                sourceFile.readFully(buffer);


                int numbersInBuffer = (int) Math.ceil(buffer.length * 1.0 / BLOCKSIZE);
                byte[][] encryptedData = new byte[SHAREHOLDERS][numbersInBuffer * SHARESIZE];
                byte[] oneNumber;

                for (int i = 0; i < numbersInBuffer; i++) {
                    if ((i + 1) * BLOCKSIZE <= buffer.length) {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCKSIZE, (i + 1) * BLOCKSIZE);
                    } else {
                        oneNumber = Arrays.copyOfRange(buffer, i * BLOCKSIZE, buffer.length);
                        System.out.println(DatatypeConverter.printHexBinary(oneNumber));

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
                processed += buffer.length;
            }
        } catch (IOException e) {
            System.out.println(sourceEndingByte);
            e.printStackTrace();
        }
    }
}
