package de.tu_darmstadt.Encryption;

import de.tu_darmstadt.BigIntegerPolynomial;

import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.Arrays;

import static de.tu_darmstadt.Parameters.*;

/**
 * Created by stathis on 5/31/17.
 */
public class EncryptionTask implements Runnable {
    private int offset;
    private byte[] data;
    private RandomAccessFile[] outs;

    public EncryptionTask(byte[] data, int offset, RandomAccessFile[] outs) {
        this.data = data;
        this.offset = offset;
        this.outs = outs;

    }


    public void run() {
        int encodedSize = (int) Math.ceil(data.length * 1.0 / BLOCKSIZE);
        byte[][] encryptedData = new byte[SHAREHOLDERS][encodedSize * SHARESIZE];
        byte[] oneNumber;
        try {
            for (int i = 0; i < encodedSize; i++) {
                if ((i + 1) * BLOCKSIZE <= data.length) {
                    oneNumber = Arrays.copyOfRange(data, i * BLOCKSIZE, (i + 1) * BLOCKSIZE);
                } else {
                    oneNumber = Arrays.copyOfRange(data, i * BLOCKSIZE, data.length);
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
                synchronized (this) {
                    //System.out.println("Thread: " + offset+ " writing " + encryptedData[j].length +" Bytes to file " + j);
                    outs[j].seek(encryptedData[j].length * offset + 18 + MODSIZE);
                    outs[j].write(encryptedData[j]);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}


