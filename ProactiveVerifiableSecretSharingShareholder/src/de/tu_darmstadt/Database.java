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
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Semaphore;

import static de.tu_darmstadt.Parameters.*;

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
            new File(LOCAL_DIR + SERVER_NAME + "/").mkdir();
            String databaseUrl = "jdbc:sqlite:" + LOCAL_DIR + SERVER_NAME + "/shares.db";
            // create a connection source to the database
            connectionSource = new JdbcConnectionSource(databaseUrl);

            // instantiate the daos
            shareholdersDao = DaoManager.createDao(connectionSource, ShareHolder.class);
            sharesDao = DaoManager.createDao(connectionSource, Share.class);
            manyToManyDao = DaoManager.createDao(connectionSource, ManyToMany.class);
            pedersenParametersDao = DaoManager.createDao(connectionSource, PedersenParameters.class);

            if (!new File(LOCAL_DIR + SERVER_NAME + "/shares.db").isFile()) {
                TableUtils.createTable(connectionSource, ShareHolder.class);
                TableUtils.createTable(connectionSource, Share.class);
                TableUtils.createTable(connectionSource, PedersenParameters.class);
                TableUtils.createTable(connectionSource, ManyToMany.class);
                PedersenParameters params = new PedersenParameters();
                params.encodingBits = 504;
                params.id = "params";
                params.p = new BigInteger("5332275656290381761151198530729233982045321085515139627964831008112763881518456822427005798835628805933893797788055798998828455763667455803776408064701108263671205140461194474071819058893028574502135215643043380094791062121714285044320527620453186221677585340281490457426165747439193685558532639828794151660419560720834965739612358595748294036843087699883160158944123208439891357237492603158475613629457891462903201185561539335567339369120506341184649845573184732601652987255272200397725039863926858661478643913974390004185165265430537677575218591792103659168701785773774722936101623740432213103959769619913214369587507103080123826566877634723963911631915056433280384896552292472412611287982338899314131559938757399883079864722202744566762221577395000750754547957363979106956768831115782416126491700252642450928000598788380406126104269577705997147323812262011446146321659123559105527827387590628481131833565204812085662526579");
                params.q = new BigInteger("11395573354440450526618175109837529537306347259126370360080603622252608891685157071130010346881667566366371772441935335277596868580233191484422155539478519");
                params.g = new BigInteger("2648350193592649761779883200579540893968028666865685599409109460484365339527086612803267768313680501460078361376455353266853087638379930400375930187379987047077113326137914514063069883004429962015103147022988671444249296832671245983852767556757502498374764219246665027669793938595964925236308451692645199979131402404917988370045261685416962121813858936442880622268024924692671738096188292456470627939406170413246422599379068082098946109940043999218048513304431142200648747897233492074163751968883874002290800391048357644687812284888561161865981504122459850105301684291983776263581707046205454564246964598663605189443512699242426436091029792937825468298740683604269933852926601603979732751448628416794064958248002630714563994777373598985086505787679864645529675006325047001233126416480308581954678198017010391951442356522119208978780330962721576087441548589010640976078057355306483649958428830581660276358173483746199925577565");
                params.h = new BigInteger("1646305938548563879106051297571377923395823957068124447572681406250804117741734276670782550108377949866960203021182475926106220749693847212239920299764239317539659105720472735990950581549489613357138183509173130241574405679253466443807408983133991538828824445638724568330340353916612675117174132822975592903658235567700090143042294377195231965905990785678310224217460662310414672459085944713336934910380171935042381404775827091410921380150179818280013175351285873515931556934054915544353922651829389078113958520710046008549051886440219561595487090445095437093662025996358984299870404201400972394928056149825207718794407869190038886026183784700310998625981782457262991756966791370087281941018016474207791624136839223383283070249188701022326140644300138061542815951380503964156495260063199929823647797098102768107519921469390531358423547312262618459442022484632454736402558374040118865096227168072384083994868921189210117318543");
                pedersenParametersDao.createIfNotExists(params);
                for (int i = 1; i<=10; i++){
                    ShareHolder shareHolder = new ShareHolder("Server"+i, "localhost", 8000+(i-1)*10+SERVER_NR+1);
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
        manyToManyQb.where().eq(ManyToMany.SHARE_ID_FIELD_NAME, share.getName()).and().eq(ManyToMany.SHAREHOLDER_ID_FIELD_NAME, shareHolder.getName());
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
