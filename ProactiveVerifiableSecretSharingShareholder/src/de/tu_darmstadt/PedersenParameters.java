package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.math.BigInteger;
@DatabaseTable(tableName = "PedersenParameters")
public class PedersenParameters implements Serializable {

    @DatabaseField(id=true)
    public String id;

    @DatabaseField
    int encodingBits = 504;
    @DatabaseField
    public BigInteger p;
    @DatabaseField
    public BigInteger q;
    @DatabaseField
    public BigInteger g;
    @DatabaseField
    public BigInteger h;

    public PedersenParameters(){};

    public PedersenParameters(BigInteger p, BigInteger q, BigInteger g, BigInteger h) {
        this.p = p;
        this.q = q;
        this.g = g;
        this.h = h;
        this.id = "params";
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getH() {
        return h;
    }

}
