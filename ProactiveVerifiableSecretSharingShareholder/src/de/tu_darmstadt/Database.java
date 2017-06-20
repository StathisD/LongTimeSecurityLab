package de.tu_darmstadt;

import static de.tu_darmstadt.Parameters.*;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.Semaphore;

/**
 * Created by Stathis on 6/19/17.
 */
public class Database {

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

            if (!new File("shares.db").isFile()) {
                TableUtils.createTable(connectionSource, ShareHolder.class);
                TableUtils.createTable(connectionSource, Share.class);
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

}
