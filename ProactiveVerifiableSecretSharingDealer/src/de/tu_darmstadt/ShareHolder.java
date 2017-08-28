package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.math.BigInteger;


/**
 * Created by Stathis on 6/19/17.
 */
@DatabaseTable(tableName = "shareholders")
public class ShareHolder implements Serializable{

    // we use this field-name so we can query for posts with a certain id
    public final static String ID_FIELD_NAME = "name";

    @DatabaseField(id=true, columnName = ID_FIELD_NAME)
    private String name;
    @DatabaseField
    private String ipAddress;
    @DatabaseField
    private int port;

    private BigInteger xValue;

    public ShareHolder(){}

    public ShareHolder(String name, String ipAddress, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;

    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int[] ports) {
        this.port = port;
    }


    public String toString(){
        return "NAME: " + name + " IP ADDRESS: " + ipAddress + " PORT: " + port + " X_VALUE: " + xValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigInteger getxValue() {
        return xValue;
    }

    public void setxValue(BigInteger xValue) {
        this.xValue = xValue;
    }
}