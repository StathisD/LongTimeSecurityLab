package de.tu_darmstadt.Decryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import javax.xml.bind.DatatypeConverter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


public class DecryptionTask implements Callable<Integer> {
    private byte[][] buffer;
    private long destStartingByte;


    public DecryptionTask(byte[][] buffer, long destStartingByte) {
        this.buffer = buffer;
        this.destStartingByte = destStartingByte;

    }

    @Override
    public Integer call() {
        try {
            RandomAccessFile targetFile = new RandomAccessFile(FILE_PATH + "_dec", "rw");
            int sizeLastNumber = 0;
            boolean lastBuffer = false;

            targetFile.seek(destStartingByte);
            int numbersInBuffer = buffer[0].length / SHARE_SIZE;
            BigInteger[] shares = new BigInteger[NEEDED_SHARES];
            byte[] decryptedData = new byte[numbersInBuffer*BLOCK_SIZE];

            if (buffer[0].length < BUFFER_SIZE) {
                lastBuffer = true;
                sizeLastNumber = (int) (TARGET_FILE_SIZE % BLOCK_SIZE);
                decryptedData = new byte[(int)(TARGET_FILE_SIZE - destStartingByte)];
            }

            for (int i = 0; i<numbersInBuffer; i++) {
                // reconstruct number
                for (int j = 0; j < NEEDED_SHARES; j++) {
                    byte[] oneNumber = Arrays.copyOfRange(buffer[j], i * SHARE_SIZE, (i + 1) * SHARE_SIZE);
                    shares[j] = new BigInteger(1, oneNumber);
                }

                //decrypt
                BigInteger decryptedNumber = BigIntegerPolynomial.interpolate(shares, MODULUS);
                byte[] decryptedBytes = decryptedNumber.toByteArray();

                if (i == numbersInBuffer - 1 && lastBuffer) {
                    decryptedBytes = fixLength(decryptedBytes, sizeLastNumber);
                } else {
                    decryptedBytes = fixLength(decryptedBytes, BLOCK_SIZE);
                }
                System.arraycopy( decryptedBytes, 0, decryptedData, i * BLOCK_SIZE, decryptedBytes.length);

            }


            targetFile.write(decryptedData);
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
