package de.tu_darmstadt;

import java.math.BigInteger;
import java.security.SecureRandom;

import static de.tu_darmstadt.Parameters.*;


/**
 * Created by stathis on 6/3/17.
 */

public class BigIntegerPolynomial {
    //Verification
    private static BigInteger[] lagrangeCoefficients;
    public BigInteger[] commitments;
    public BigIntegerPolynomial G;
    private BigInteger[] coefficients;
    private int degree;
    private BigInteger modulus;

    public BigIntegerPolynomial(BigInteger[] coeff, BigInteger modulus) {
        this.degree = coeff.length - 1;
        this.coefficients = coeff;
        this.modulus = modulus;
    }

    public BigIntegerPolynomial(int degree, BigInteger modulus, BigInteger a0) {
        a0 = a0.mod(modulus);
        this.degree = degree;
        this.modulus = modulus;
        coefficients = new BigInteger[degree + 1];
        commitments = new BigInteger[degree + 1];
        BigInteger[] commitmentSeeds = new BigInteger[degree + 1];
        PedersenCommitment[] pedersenCommitments = new PedersenCommitment[degree + 1];

        coefficients[0] = a0;

        if(VERIFIABILITY){
            pedersenCommitments[0] = committer.commit(a0);
            commitmentSeeds[0] = pedersenCommitments[0].getSecret();
            commitments[0] = pedersenCommitments[0].getCommitment();
        }

        for (int i = 1; i <= degree; i++) {
            BigInteger ai = (new BigInteger(Parameters.MOD_LENGTH, new SecureRandom())).mod(modulus);
            coefficients[i] = ai;

            if (VERIFIABILITY){
                pedersenCommitments[i] = committer.commit(ai);
                commitmentSeeds[i] = pedersenCommitments[i].getSecret();
                commitments[i] = pedersenCommitments[i].getCommitment();
            }
        }

        if (VERIFIABILITY){
            this.G = new BigIntegerPolynomial(commitmentSeeds, modulus);
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


    public static boolean verifyCommitment(BigInteger shareIndex, BigInteger share, BigInteger shareCommitment, BigInteger[] publicCommitments, BigInteger p) {

        BigInteger commitment = BigInteger.ONE;
        for (int i = 0; i < publicCommitments.length; i++) {
            commitment = commitment.multiply(publicCommitments[i].modPow(shareIndex.modPow(BigInteger.valueOf(i),p),p));
        }
        commitment = commitment.mod(p);

        BigInteger com = committer.generateCommitment(share, shareCommitment);
        return com.equals(commitment);
    }

    public BigInteger evaluatePolynom(BigInteger value) {
        BigInteger result = BigInteger.ZERO;
        for (int i = 0; i < coefficients.length; i++) {
            BigInteger index = BigInteger.valueOf(i);
            BigInteger coefficient = coefficients[i];
            result = result.add(coefficient.multiply(value.modPow(index, modulus)));
        }
        return result.mod(modulus);
    }
}
