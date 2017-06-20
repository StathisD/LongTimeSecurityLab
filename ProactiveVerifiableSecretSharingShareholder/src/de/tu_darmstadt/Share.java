package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigInteger;

/**
 * Created by Stathis on 6/19/17.
 */
@DatabaseTable(tableName = "shares")
public class Share {

    // we use this field-name so we can query for posts with a certain id
    public final static String ID_FIELD_NAME = "id";

    @DatabaseField(id = true, columnName = ID_FIELD_NAME)
    private String name;
    @DatabaseField
    private int xValue;
    @DatabaseField
    private String sourceIp;
    @DatabaseField
    private int sourcePort;
    @DatabaseField
    private BigInteger modulus;
    @DatabaseField
    private int numberOfShareholders;
    @DatabaseField
    private int neededShares;

    public Share(){}

    public Share(String name, int xValue, String sourceIp, int sourcePort, BigInteger modulus, int numberOfShareholders, int neededShares){
        this.name = name;
        this.xValue = xValue;
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.modulus = modulus;
        this.numberOfShareholders = numberOfShareholders;
        this.neededShares = neededShares;
    }

    public String getName(){
        return name;
    }

    public int getxValue(){
        return xValue;
    }

    public String getSourceIp(){
        return sourceIp;
    }

    public int getSourceport(){
        return sourcePort;
    }

    public BigInteger getModulus(){
        return modulus;
    }

    public int getNeededShares() {
        return neededShares;
    }

    public void setNeededShares(int neededShares) {
        this.neededShares = neededShares;
    }

    public int getNumberOfShareholders() {
        return numberOfShareholders;
    }

    public void setNumberOfShareholders(int numberOfShareholders) {
        this.numberOfShareholders = numberOfShareholders;
    }
}
