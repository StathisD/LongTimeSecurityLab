package de.tu_darmstadt.Decryption;

import de.tu_darmstadt.BigIntegerPolynomial;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static de.tu_darmstadt.Constants.*;

/**
 * Created by stathis on 5/31/17.
 */
public class DecryptionTask implements Callable<byte[][]> {
    private byte[] data;

    public DecryptionTask(byte[] data) {
        this.data = data;

    }

    private static byte[] fixLength(byte[] data) {

        byte[] newData = new byte[MODSIZE];
        //add Padding if needed
        if (data.length < MODSIZE) {
            System.arraycopy(data, 0, newData, (newData.length - data.length), data.length);
        } else if (data.length > MODSIZE) {
            System.arraycopy(data, 1, newData, 0, MODSIZE);
        } else {
            newData = data;
        }
        return newData;
    }

    public byte[][] call() {
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

                for (int x = 1; x <= SHAREHOLDERS; x++) {
                    BigInteger xValue = BigInteger.valueOf(x);
                    BigInteger yValue = polynomial.evaluate(xValue);
                    byte[] xValueBytes = xValue.toByteArray();
                    byte[] yValueBytes = fixLength(yValue.toByteArray());
                    byteShare = concat(xValueBytes, yValueBytes);
                    //System.out.println(DatatypeConverter.printHexBinary(byteShare));
                    System.arraycopy(byteShare, 0, encryptedData[(x - 1)], i * SHARESIZE, byteShare.length);
                }


                /*
                //decrypt
                BigInteger encryptedNumber = number;//BigIntegerPolynomial.interpolate(shares,SHAREHOLDERS, BigInteger.ZERO, MODULUS);

                byte[] encNumber = encryptedNumber.toByteArray();

                if (!Arrays.equals(oneNumber, encNumber)) {

                    System.out.println(number);
                    System.out.println(encryptedNumber);

                    System.out.println("Dec: " + DatatypeConverter.printHexBinary(oneNumber));
                    System.out.println("Enc: " + DatatypeConverter.printHexBinary(encNumber));
                    break;
                }*/


            }
            /*for (int j = 0; j < SHAREHOLDERS; j++) {
                System.out.println(DatatypeConverter.printHexBinary(encryptedData[j]));
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedData;
    }


}


