package de.tu_darmstadt.Encryption;

import de.tu_darmstadt.BigIntegerPolynomial;
import de.tu_darmstadt.Constants;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.concurrent.Callable;

/**
 * Created by stathis on 5/31/17.
 */
public class EncryptionTask implements Callable<byte[]> {
    private int bits;
    private int blockLength;
    private byte[] data;

    public EncryptionTask(byte[] data, int bits) {
        this.data = data;
        this.bits = bits;
        this.blockLength = bits / 8;
    }

    public byte[] call() {
        int encodedSize = (int) Math.ceil(data.length * 1.0 / blockLength);
        byte[] encryptedData = new byte[data.length];
        byte[] oneNumber;
        try {
            for (int i = 0; i < encodedSize; i++) {
                if ((i + 1) * blockLength <= data.length) {
                    oneNumber = Arrays.copyOfRange(data, i * blockLength, (i + 1) * blockLength);
                } else {
                    oneNumber = Arrays.copyOfRange(data, i * blockLength, data.length);
                }
                /*
                    System.arraycopy(oneNumber, 0, newData, (newData.length - oneNumber.length), oneNumber.length);
                    oneNumber = newData;
                }*/
                BigInteger number = new BigInteger(1, oneNumber);
                //encrypt
                BigIntegerPolynomial polynomial = new BigIntegerPolynomial(Constants.SHAREHOLDERS - 1, Constants.MODULUS, number);
                TreeMap<BigInteger, BigInteger> shares = new TreeMap<>();
                for (int j = 1; j <= Constants.SHAREHOLDERS; j++) {
                    BigInteger value = BigInteger.valueOf(j);
                    shares.put(value, polynomial.evaluate(value));
                }

                //decrypt
                BigInteger encryptedNumber = number;//BigIntegerPolynomial.interpolate(shares,Constants.SHAREHOLDERS, BigInteger.ZERO, Constants.MODULUS);

                byte[] encNumber = encryptedNumber.toByteArray();

                //add Padding if needed
                if (encNumber.length < oneNumber.length) {
                    //int sign = encryptedNumber.signum();
                    byte[] newData = new byte[oneNumber.length];
                    //if (sign == -1) Arrays.fill(newData, (byte) 0xff);
                    System.arraycopy(encNumber, 0, newData, (newData.length - encNumber.length), encNumber.length);
                    encNumber = newData;
                } else if (encNumber.length > oneNumber.length) {
                    byte[] newData = new byte[oneNumber.length];
                    System.arraycopy(encNumber, 1, newData, 0, newData.length);
                    encNumber = newData;
                }

                if (!Arrays.equals(oneNumber, encNumber)) {

                    System.out.println(number);
                    System.out.println(encryptedNumber);

                    System.out.println("Dec: " + DatatypeConverter.printHexBinary(oneNumber));
                    System.out.println("Enc: " + DatatypeConverter.printHexBinary(encNumber));
                    break;
                }

                System.arraycopy(encNumber, 0, encryptedData, i * blockLength, encNumber.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedData;
    }
}


