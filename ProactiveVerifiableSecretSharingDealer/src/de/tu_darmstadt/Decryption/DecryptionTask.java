package de.tu_darmstadt.Decryption;

/**
 * Created by stathis on 6/4/17.
 */

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Parameters.*;


public class DecryptionTask implements Callable {
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
            if (buffer[0].length < BUFFER_SIZE) {
                lastBuffer = true;
                sizeLastNumber = (int) (TARGET_FILE_SIZE % BLOCK_SIZE);
            }

            targetFile.seek(destStartingByte);
            int numbersInBuffer = buffer[0].length / SHARE_SIZE;

            BigInteger[] shares = new BigInteger[NEEDED_SHARES];

            for (int i = 0; i<numbersInBuffer; i++) {
                // reconstruct number
                for (int j = 0; j < NEEDED_SHARES; j++) {
                    byte[] oneNumber = Arrays.copyOfRange(buffer[j], i * SHARE_SIZE, (i + 1) * SHARE_SIZE);
                    BigInteger yValue = new BigInteger(1, oneNumber);
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

                targetFile.write(decryptedBytes);
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
