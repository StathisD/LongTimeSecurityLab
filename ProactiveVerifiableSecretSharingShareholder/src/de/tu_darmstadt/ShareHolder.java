package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;


/**
 * Created by Stathis on 6/19/17.
 */
@DatabaseTable(tableName = "shareholders")
public class ShareHolder implements Serializable{

    // we use this field-name so we can query for posts with a certain id
    public final static String ID_FIELD_NAME = "ipAddress";

    @DatabaseField(id=true, columnName = ID_FIELD_NAME)
    private String ipAddress;
    @DatabaseField
    private int port;

    public ShareHolder(){}

    public ShareHolder(String ipAddress, int port){
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
        return  "IP ADDRESS: " + ipAddress + " PORT: " + port;
    }
}