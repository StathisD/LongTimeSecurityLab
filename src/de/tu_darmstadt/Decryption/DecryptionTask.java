package de.tu_darmstadt.Decryption;

import de.tu_darmstadt.BigIntegerPolynomial;

import javax.xml.bind.DatatypeConverter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.TreeMap;

import static de.tu_darmstadt.Parameters.*;

/**
 * Created by stathis on 5/31/17.
 */
public class DecryptionTask implements Runnable {
    private int offset;
    private RandomAccessFile[] ins;
    private RandomAccessFile out;

    public DecryptionTask(int offset, RandomAccessFile[] ins, RandomAccessFile out) {
        this.offset = offset;
        this.ins = ins;
        this.out = out;
    }

    public void run() {
        try {
            int nGet;
            RandomAccessFile test = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/testFile", "r");

            while (ins[0].getFilePointer() < ins[0].length()) {

                nGet = (int) Math.min(SHARESIZE, ins[0].length() - ins[0].getFilePointer());

                TreeMap<BigInteger, BigInteger> shares = new TreeMap<>();
                for (int j = 0; j < SHAREHOLDERS; j++) {
                    final byte[] byteArray = new byte[nGet];
                    ins[j].readFully(byteArray);
                    byte[] xValueBytes = new byte[1];
                    byte[] yValueBytes = new byte[MODSIZE];
                    xValueBytes[0] = byteArray[0];
                    System.arraycopy(byteArray, 1, yValueBytes, 0, MODSIZE);
                    BigInteger xValue = new BigInteger(1, xValueBytes);
                    BigInteger yValue = new BigInteger(1, yValueBytes);
                    shares.put(xValue, yValue);
                }
                //System.out.println(shares);
                byte[] testByte = new byte[BLOCKSIZE];
                test.readFully(testByte);
                BigInteger number = new BigInteger(1, testByte);


                //decrypt
                BigInteger decryptedNumber = BigIntegerPolynomial.interpolate(shares, SHAREHOLDERS, BigInteger.ZERO, MODULUS);
                byte[] decryptedBytes = decryptedNumber.toByteArray();
                decryptedBytes = fixLength(decryptedBytes, BLOCKSIZE);

                //out.write(decryptedBytes);
                System.out.println(DatatypeConverter.printHexBinary(decryptedBytes));

                System.out.println(DatatypeConverter.printHexBinary(testByte));
                break;


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


