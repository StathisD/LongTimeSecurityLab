package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */
public class BigIntegerPolynomial {
    private static BigInteger[] lagrangeCoefficients;
    private BigInteger[] coefficients;
    private int degree;
    private BigInteger modulus;

    public BigIntegerPolynomial(int degree, BigInteger modulus, BigInteger a0) {
        this.degree = degree;
        this.modulus = modulus;
        coefficients = new BigInteger[degree + 1];
        coefficients[0] = a0.mod(modulus);
        for (int i = 1; i <= degree; i++) {
            BigInteger ai = (new BigInteger(Parameters.MOD_LENGTH, new Random())).mod(modulus);
            coefficients[i] = ai;
        }
    }

    public static BigInteger interpolate(BigInteger[] points, BigInteger modulus) {
        BigInteger result = BigInteger.ZERO;
        for (int j = 0; j < points.length; j++) {
            BigInteger yj = points[j];
            result = result.add(yj.multiply(lagrangeCoefficients[j]));
        }
        return result.mod(modulus);
    }

    public static void computeLagrangeCoefficients(BigInteger[] xValues, BigInteger modulus) {
        lagrangeCoefficients = new BigInteger[xValues.length];
        for (int j = 0; j < xValues.length; j++) {
            BigInteger xj = xValues[j];
            BigInteger lj = BigInteger.ONE;
            for (int m = 0; m < xValues.length; m++) {
                BigInteger xm = xValues[m];
                if (!xj.equals(xm)) {
                    BigInteger product = xm.multiply(xm.subtract(xj).modInverse(modulus));
                    lj = lj.multiply(product);
                }
            }
            lagrangeCoefficients[j] = lj.mod(modulus);
        }
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
