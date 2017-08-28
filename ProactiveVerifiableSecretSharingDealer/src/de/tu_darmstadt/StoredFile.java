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
    private String fileLocation;
    @DatabaseField
    private BigInteger modulus;
    @DatabaseField
    private int numberOfShareholders;
    @DatabaseField
    private int neededShares;
    @DatabaseField
    private long fileSize;


    public StoredFile(){}

    public StoredFile(String name, String fileLocation, BigInteger modulus, int numberOfShareholders, int neededShares, long fileSize) {
        this.name = name;
        this.fileLocation = fileLocation;
        this.modulus = modulus;
        this.numberOfShareholders = numberOfShareholders;
        this.neededShares = neededShares;
        this.fileSize = fileSize;
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

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }
}
