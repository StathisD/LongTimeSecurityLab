package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

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

    public static BigInteger interpolate(TreeMap<BigInteger, BigInteger> points, int neededPoints, BigInteger position, BigInteger modulus) {
        if (points.size() < neededPoints) {
            // no interpolation possible
            return BigInteger.valueOf(-1);
        } else {
            BigInteger result = BigInteger.ZERO;
            for (Map.Entry<BigInteger, BigInteger> entry : points.entrySet()) {
                BigInteger xj = entry.getKey();
                BigInteger yj = entry.getValue();
                BigInteger lj = BigInteger.ONE;
                for (BigInteger x : points.keySet()) {
                    if (!xj.equals(x)) {
                        BigInteger nominator = position.subtract(x);
                        BigInteger denominator = xj.subtract(x);
                        BigInteger product = nominator.multiply(denominator.modInverse(modulus)).mod(modulus);
                        lj = lj.multiply(product).mod(modulus);
                    }
                }
                //System.out.println(lj);
                result = result.add(yj.multiply(lj)).mod(modulus);
            }
            return result;
        }
    }

    public BigInteger evaluate(BigInteger value) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < coefficients.length; i++) {
            BigInteger index = BigInteger.valueOf(i);
            BigInteger coefficient = coefficients[i];
            result = result.add(coefficient.multiply(value.modPow(index, modulus)).mod(modulus)).mod(modulus);
        }
        return result;
    }
}
