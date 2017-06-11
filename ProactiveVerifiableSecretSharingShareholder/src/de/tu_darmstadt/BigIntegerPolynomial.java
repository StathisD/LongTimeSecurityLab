package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by stathis on 6/3/17.
 */

public class BigIntegerPolynomial {

    //Verification
    static BigInteger g;
    static BigInteger h;


    public static BigInteger computeCommitment( BigInteger a, BigInteger b, BigInteger modulus){
        return g.modPow(a, modulus).multiply(h.modPow(b,modulus)).mod(modulus);
    }

    public static boolean verifyCommitment(BigInteger shareIndex, BigInteger share, BigInteger shareCommitment, BigInteger publicCommitment, BigInteger modulus){
        BigInteger com = computeCommitment(share, shareCommitment, modulus);
        return com.equals(publicCommitment.modPow(shareIndex, modulus));
    }
}