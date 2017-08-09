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
    // needs_renewal, in_progress, renewed
    @DatabaseField
    private String renewStatus;
    @DatabaseField
    private long lastRenewed;
    @DatabaseField
    private long numbersInShare;


    public Share(){}

    public Share(String name, int xValue, String sourceIp, int sourcePort, BigInteger modulus, int numberOfShareholders, int neededShares, long numbersInShare){
        this.name = name;
        this.xValue = xValue;
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.modulus = modulus;
        this.numberOfShareholders = numberOfShareholders;
        this.neededShares = neededShares;
        this.numbersInShare = numbersInShare;
        this.renewStatus = "renewed";
        this.lastRenewed = System.currentTimeMillis();
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

    public String getRenewStatus() {
        return renewStatus;
    }

    public void setRenewStatus(String renewStatus) {
        this.renewStatus = renewStatus;
    }

    public long getLastRenewed() {
        return lastRenewed;
    }

    public void setLastRenewed(long lastRenewed) {
        this.lastRenewed = lastRenewed;
    }

    public long getNumbersInShare() {
        return numbersInShare;
    }

    public void setNumbersInShare(long numbersInShare) {
        this.numbersInShare = numbersInShare;
    }
}
