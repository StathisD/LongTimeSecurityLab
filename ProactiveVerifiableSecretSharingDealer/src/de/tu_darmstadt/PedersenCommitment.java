package de.tu_darmstadt;

import java.math.BigInteger;

public class PedersenCommitment {

    private final PedersenParameters params;
    private final BigInteger commitment;
    private BigInteger secret;

    public PedersenCommitment(PedersenParameters params, BigInteger commitment, BigInteger secret) {
        this.params = params;
        this.commitment = commitment;
        this.secret = secret;
    }

    public BigInteger getCommitment() {
        return commitment;
    }

    public BigInteger getSecret() {
        return secret;
    }

    public void setSecret(BigInteger secret) {
        this.secret = secret;
    }

    public PedersenParameters getParams() {
        return params;
    }

}
