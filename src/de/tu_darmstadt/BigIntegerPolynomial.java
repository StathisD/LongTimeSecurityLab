package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public class BigIntegerPolynomial {
    private BigInteger[] coefficients;
    private int degree;
    private BigInteger modulus;

    public BigIntegerPolynomial(int degree, BigInteger modulus, BigInteger a0) {
        this.degree = degree;
        this.modulus = modulus;
        coefficients = new BigInteger[degree + 1];
        coefficients[0] = a0.mod(modulus);
        for (int i = 1; i <= degree; i++) {
            BigInteger ai = (new BigInteger(Parameters.MODLENGTH, new Random())).mod(modulus);
            coefficients[i] = ai;
        }
    }
/*
    public static BigInteger interpolate(BigInteger[][] points, int neededPoints, BigInteger position, BigInteger modulus) {
        if (points.length < neededPoints) {
            // no interpolation possible
            return BigInteger.valueOf(-1);
        } else {
            BigInteger result = BigInteger.ZERO;
            for (int i = 0; i < points.length; i++) {
                BigInteger xj = points[i][0];
                BigInteger yj = points[i][1];
                BigInteger lj = BigInteger.ONE;
                for (int j = 0; j < points.length; j++) {
                    BigInteger x = points[j][0];
                    if (!xj.equals(x)) {
                        BigInteger nominator = position.subtract(x);
                        BigInteger denominator = xj.subtract(x);
                        BigInteger product = nominator.multiply(denominator.modInverse(modulus));
                        lj = lj.multiply(product);
                    }
                }
                result = result.add(yj.multiply(lj));
            }
            return result.mod(modulus);
        }
    }*/

    public static BigInteger interpolate(BigInteger[][] points, BigInteger modulus) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < points.length; i++) {
            BigInteger xj = points[i][0];
            BigInteger yj = points[i][1];
            BigInteger lj = BigInteger.ONE;
            for (int j = 0; j < points.length; j++) {
                BigInteger x = points[j][0];
                if (!xj.equals(x)) {
                    BigInteger product = x.multiply(x.subtract(xj).modInverse(modulus));
                    lj = lj.multiply(product);
                }
            }
            result = result.add(yj.multiply(lj));
        }
        return result.mod(modulus);
    }

    public BigInteger evaluate(BigInteger value) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < coefficients.length; i++) {
            BigInteger index = BigInteger.valueOf(i);
            BigInteger coefficient = coefficients[i];
            result = result.add(coefficient.multiply(value.modPow(index, modulus)));
        }
        return result.mod(modulus);
    }
}
