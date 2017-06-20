package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigInteger;

/**
 * Created by Stathis on 6/19/17.
 */
@DatabaseTable(tableName = "storedFiles")
public class StoredFile {

    @DatabaseField(id = true)
    private String name;
    @DatabaseField
    private BigInteger modulus;
    @DatabaseField
    private int numberOfShareholders;
    @DatabaseField
    private int neededShares;
    @DatabaseField
    private long fileSize;
    @DatabaseField
    private int bits;

    public StoredFile(){}

    public StoredFile(String name, BigInteger modulus, int numberOfShareholders, int neededShares, long fileSize, int bits){
        this.name = name;
        this.modulus = modulus;
        this.numberOfShareholders = numberOfShareholders;
        this.neededShares = neededShares;
        this.fileSize = fileSize;
        this.bits = bits;
    }

    public String getName(){
        return name;
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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }
}
