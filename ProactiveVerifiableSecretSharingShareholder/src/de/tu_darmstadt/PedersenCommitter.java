package de.tu_darmstadt;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

public class PedersenCommitter {
    
    BigInteger p;
    BigInteger q;
    BigInteger g;
    BigInteger h;
    
    public PedersenCommitter(PedersenParameters params) {
        this.p = params.getP();
        this.q = params.getQ();
        this.g = params.getG();
        this.h = params.getH();
    }
    
    private BigInteger generateGqElement() {
        Random rnd = new SecureRandom();
        
        BigInteger x;
        do {
            x = new BigInteger(q.bitLength(), rnd);
        } while (x.modPow(q, p).compareTo(BigInteger.ONE)!=0);
        
        return x;
    }
    
    private BigInteger generateZqElement() {
        Random rnd = new SecureRandom();
        
        BigInteger x;
        do {
            x = new BigInteger(q.bitLength(), rnd);
        } while (x.compareTo(q)>=0);
        
        return x;
    }
    
    public PedersenCommitment commit(BigInteger number) {

        BigInteger secret = generateZqElement();

        BigInteger commitment = generateCommitment(number, secret);
        
        return new PedersenCommitment(new PedersenParameters(p, q, g, h), commitment, secret);
    }

    
    public BigInteger generateCommitment(BigInteger message, BigInteger secret) {
        BigInteger commitment;
        BigInteger s = message;
        BigInteger t = secret;
        commitment = g.modPow(s, p).multiply(h.modPow(t, p)).mod(p);

        return commitment.mod(p);
    }
    
    public boolean isRevealed(PedersenCommitment commitment, BigInteger number) {
        BigInteger secret = commitment.getSecret();
        BigInteger com1 = commitment.getCommitment();
        BigInteger com2 = generateCommitment(number.mod(p), secret);
        return com1.equals(com2);
    }
    
    public PedersenParameters getParams() {
        return new PedersenParameters(p, q, g, h);
    }
}
