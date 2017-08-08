package de.tu_darmstadt;

import static de.tu_darmstadt.Parameters.*;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by Stathis on 6/19/17.
 */
public class Database {

    private static PreparedQuery<ShareHolder> shareHoldersForShareQuery = null;
    private static PreparedQuery<Share> sharesForShareHolderQuery = null;

    /**
     * Connect to a sample database
     */
    public static void initiateDb() {
        ConnectionSource connectionSource = null;
        try{
            String databaseUrl = "jdbc:sqlite:shares.db";
            // create a connection source to the database
            connectionSource = new JdbcConnectionSource(databaseUrl);

            // instantiate the daos
            shareholdersDao = DaoManager.createDao(connectionSource, ShareHolder.class);
            sharesDao = DaoManager.createDao(connectionSource, Share.class);
            manyToManyDao = DaoManager.createDao(connectionSource, ManyToMany.class);
            pedersenParametersDao = DaoManager.createDao(connectionSource, PedersenParameters.class);

            if (!new File("shares.db").isFile()) {
                TableUtils.createTable(connectionSource, ShareHolder.class);
                TableUtils.createTable(connectionSource, Share.class);
                TableUtils.createTable(connectionSource, PedersenParameters.class);
                TableUtils.createTable(connectionSource, ManyToMany.class);
                for (int i = 1; i<=10; i++){
                    ShareHolder shareHolder = new ShareHolder("localhost", 8000+i);
                    shareholdersDao.createIfNotExists(shareHolder);
                }
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

    public static BigInteger lookupXvalueForShareHolderAndShare(ShareHolder shareHolder, Share share) throws SQLException{
        QueryBuilder<ManyToMany, String> manyToManyQb = manyToManyDao.queryBuilder();
        manyToManyQb.selectColumns(ManyToMany.X_VALUE);
        manyToManyQb.where().eq(ManyToMany.SHARE_ID_FIELD_NAME, share.getName()).and().eq(ManyToMany.SHAREHOLDER_ID_FIELD_NAME, shareHolder.getIpAddress());
        PreparedQuery<ManyToMany> query= manyToManyQb.prepare();
        ManyToMany manyToMany = manyToManyDao.query(query).get(0);
        return manyToMany.getxValue();
    }

    public static List<ShareHolder> lookupShareHoldersForShare(Share share) throws SQLException {
        if (shareHoldersForShareQuery == null) {
            shareHoldersForShareQuery = makeShareHoldersForShareQuery();
        }
        shareHoldersForShareQuery.setArgumentHolderValue(0, share);
        show(shareholdersDao.query(shareHoldersForShareQuery));
        return shareholdersDao.query(shareHoldersForShareQuery);
    }

    public static List<Share> lookupSharesForShareHolder(ShareHolder shareHolder) throws SQLException {
        if (sharesForShareHolderQuery == null) {
            sharesForShareHolderQuery = makeSharesForShareHolderQuery();
        }
        sharesForShareHolderQuery.setArgumentHolderValue(0, shareHolder);
        return sharesDao.query(sharesForShareHolderQuery);
    }

    /**
     * Build our query for ShareHolder objects that match a Share.
     */
    private static PreparedQuery<ShareHolder> makeShareHoldersForShareQuery() throws SQLException {
        // build our inner query for ManyToMany objects
        QueryBuilder<ManyToMany, String> manyToManyQb = manyToManyDao.queryBuilder();
        // just select the shareHolder-id field
        manyToManyQb.selectColumns(ManyToMany.SHAREHOLDER_ID_FIELD_NAME);

        SelectArg shareSelectArg = new SelectArg();
        // you could also just pass in share1 here
        manyToManyQb.where().eq(ManyToMany.SHARE_ID_FIELD_NAME, shareSelectArg);

        // build our outer query for ShareHolder objects
        QueryBuilder<ShareHolder, String> shareHolderQb = shareholdersDao.queryBuilder();
        // where the id matches in the shareHolder-id from the inner query
        shareHolderQb.where().in(ShareHolder.ID_FIELD_NAME, manyToManyQb);
        return shareHolderQb.prepare();
    }

    /**
     * Build our query for Share objects that match a ShareHolder
     */
    private static PreparedQuery<Share> makeSharesForShareHolderQuery() throws SQLException {
        QueryBuilder<ManyToMany, String> manyToManyQb = manyToManyDao.queryBuilder();
        // this time selecting for the share-id field
        manyToManyQb.selectColumns(ManyToMany.SHARE_ID_FIELD_NAME);
        SelectArg shareHolderSelectArg = new SelectArg();
        manyToManyQb.where().eq(ManyToMany.SHAREHOLDER_ID_FIELD_NAME, shareHolderSelectArg);

        // build our outer query
        QueryBuilder<Share, String> shareQb = sharesDao.queryBuilder();
        // where the share-id matches the inner query's share-id field
        shareQb.where().in(ShareHolder.ID_FIELD_NAME, manyToManyQb);
        return shareQb.prepare();
    }

}
