package de.tu_darmstadt;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.math.BigInteger;

/**
 * Created by Stathis on 6/20/17.
 */
@DatabaseTable(tableName = "storedFilesToShareHolders")
public class ManyToMany {

    public final static String SHAREHOLDER_ID_FIELD_NAME = "shareholder_id";
    public final static String STORED_FILE_ID_FIELD_NAME = "stored_file_id";

    /**
     * This id is generated by the database and set on the object when it is passed to the create method. An id is
     * needed in case we need to update or delete this object in the future.
     */
    @DatabaseField(generatedId = true)
    int id;

    // This is a foreign object which just stores the id from the User object in this table.
    @DatabaseField(foreign = true, columnName = SHAREHOLDER_ID_FIELD_NAME)
    ShareHolder shareHolder;

    // This is a foreign object which just stores the id from the Post object in this table.
    @DatabaseField(foreign = true, columnName = STORED_FILE_ID_FIELD_NAME)
    StoredFile storedFile;

    @DatabaseField
    private BigInteger xValue;

    ManyToMany() {
        // for ormlite
    }

    public ManyToMany(ShareHolder shareHolder, StoredFile storedFile, BigInteger xValue) {
        this.shareHolder = shareHolder;
        this.storedFile = storedFile;
        this.xValue = xValue;
    }
}