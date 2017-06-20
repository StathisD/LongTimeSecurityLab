package de.tu_darmstadt;

import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Semaphore;

import static de.tu_darmstadt.Parameters.*;

/**
 * Created by Stathis on 6/19/17.
 */
public class Database {
    private static PreparedQuery<ShareHolder> shareHoldersForStoredFileQuery = null;
    private static PreparedQuery<StoredFile> storedFilesForShareHolderQuery = null;

    /**
     * Connect to a sample database
     */
    public static void initiateDb() {
        ConnectionSource connectionSource = null;
        try{
            String databaseUrl = "jdbc:sqlite:storedFiles.db";
            // create a connection source to the database
            connectionSource = new JdbcConnectionSource(databaseUrl);

            // instantiate the daos
            shareholdersDao = DaoManager.createDao(connectionSource, ShareHolder.class);
            storedFileDao = DaoManager.createDao(connectionSource, StoredFile.class);
            manyToManyDao = DaoManager.createDao(connectionSource, ManyToMany.class);

            if (!new File("storedFiles.db").isFile()) {
                TableUtils.createTable(connectionSource, ShareHolder.class);
                TableUtils.createTable(connectionSource, StoredFile.class);
                TableUtils.createTable(connectionSource, ManyToMany.class);
            }
            dbSemaphore = new Semaphore(1, true);
        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }finally {
            try {
                if (connectionSource != null) {
                    connectionSource.close();
                }
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }



    public static List<ShareHolder> lookupShareHoldersForStoredFile(StoredFile storedFile) throws SQLException {
        if (shareHoldersForStoredFileQuery == null) {
            shareHoldersForStoredFileQuery = makeShareHoldersForStoredFileQuery();
        }
        shareHoldersForStoredFileQuery.setArgumentHolderValue(0, storedFile);
        return shareholdersDao.query(shareHoldersForStoredFileQuery);
    }

    public static List<StoredFile> lookupStoredFilesForShareHolder(ShareHolder shareHolder) throws SQLException {
        if (storedFilesForShareHolderQuery == null) {
            storedFilesForShareHolderQuery = makeStoredFilesForShareHolderQuery();
        }
        storedFilesForShareHolderQuery.setArgumentHolderValue(0, shareHolder);
        return storedFileDao.query(storedFilesForShareHolderQuery);
    }

    /**
     * Build our query for ShareHolder objects that match a StoredFile.
     */
    private static PreparedQuery<ShareHolder> makeShareHoldersForStoredFileQuery() throws SQLException {
        // build our inner query for ManyToMany objects
        QueryBuilder<ManyToMany, String> manyToManyQb = manyToManyDao.queryBuilder();
        // just select the shareHolder-id field
        manyToManyQb.selectColumns(ManyToMany.SHAREHOLDER_ID_FIELD_NAME);
        SelectArg storedFileSelectArg = new SelectArg();
        // you could also just pass in storedFile1 here
        manyToManyQb.where().eq(ManyToMany.STORED_FILE_ID_FIELD_NAME, storedFileSelectArg);

        // build our outer query for ShareHolder objects
        QueryBuilder<ShareHolder, String> shareHolderQb = shareholdersDao.queryBuilder();
        // where the id matches in the shareHolder-id from the inner query
        shareHolderQb.where().in(ShareHolder.ID_FIELD_NAME, manyToManyQb);
        return shareHolderQb.prepare();
    }

    /**
     * Build our query for StoredFile objects that match a ShareHolder
     */
    private static PreparedQuery<StoredFile> makeStoredFilesForShareHolderQuery() throws SQLException {
        QueryBuilder<ManyToMany, String> manyToManyQb = manyToManyDao.queryBuilder();
        // this time selecting for the storedFile-id field
        manyToManyQb.selectColumns(ManyToMany.STORED_FILE_ID_FIELD_NAME);
        SelectArg shareHolderSelectArg = new SelectArg();
        manyToManyQb.where().eq(ManyToMany.SHAREHOLDER_ID_FIELD_NAME, shareHolderSelectArg);

        // build our outer query
        QueryBuilder<StoredFile, String> storedFileQb = storedFileDao.queryBuilder();
        // where the storedFile-id matches the inner query's storedFile-id field
        storedFileQb.where().in(ShareHolder.ID_FIELD_NAME, manyToManyQb);
        return storedFileQb.prepare();
    }

}
