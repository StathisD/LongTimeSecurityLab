package de.tu_darmstadt;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static de.tu_darmstadt.Database.lookupShareHoldersForShare;
import static de.tu_darmstadt.Database.lookupXvalueForShareHolderAndShare;
import static de.tu_darmstadt.Parameters.*;
import static de.tu_darmstadt.Parameters.SHARE_SIZE;

/**
 * Created by Stathis on 7/1/17.
 */
public class RenewShareTask{

    public static HashMap<ShareHolder, BigInteger> remoteNumberMap = new HashMap<>();

    public static HashMap<ShareHolder, BigInteger> localNumberMap = new HashMap<>();

    public static HashMap<ShareHolder, SSLConnection> sslConnectionMap = new HashMap<>();

    public static BigIntegerPolynomial currentPolynomial;

    public static long currentNumber;

    public static BigInteger[] xValues;

    private Share share;

    public RenewShareTask(Share share){
        this.share = share;
    }

    public void start(){
        try{
            ExecutorService pool = SSLClient.prepareConnections();

            dbSemaphore.acquire();
            List<ShareHolder> shareHolderList = lookupShareHoldersForShare(share);
            xValues = new BigInteger[shareHolderList.size()];
            int j = 0;
            for (ShareHolder shareholder: shareHolderList) {
                if (!sslConnectionMap.containsKey(shareholder)){
                    SSLClient sslClient = new SSLClient(shareholder, share);
                    sslConnectionMap.put(shareholder, sslClient);
                    pool.submit(sslClient);
                }
                xValues[j] = lookupXvalueForShareHolderAndShare(shareholder,share);
                j++;
            }
            dbSemaphore.release();

            MODULUS = share.getModulus();

            RandomAccessFile sourceFile = new RandomAccessFile(share.getName(), "r");
            RandomAccessFile destinationFile = new RandomAccessFile(share.getName()+"_new", "rw");
            sourceFile.seek(0L);
            destinationFile.seek(0L);
            long targetFileSize = sourceFile.length();

            initializeParameters((short) 1024, targetFileSize, 0, false);

            byte buffer[] = new byte[SHARE_SIZE];
            long numbersInShare = targetFileSize / SHARE_SIZE;


            for (long i=0; i< numbersInShare; i++) {
                remoteNumberMap = new HashMap<>();
                localNumberMap = new HashMap<>();
                currentNumber = i;
                currentPolynomial = new BigIntegerPolynomial(share.getNeededShares() - 1, share.getModulus() , BigInteger.ZERO);


                sourceFile.readFully(buffer);
                BigInteger oldNumber = new BigInteger(1, buffer);
                localNumberMap.put(new ShareHolder("",0), oldNumber);

                j = 0;
                for (ShareHolder shareholder: shareHolderList) {
                    BigInteger uj = currentPolynomial.evaluatePolynom(xValues[j]);
                    remoteNumberMap.put(shareholder, uj);
                    sslConnectionMap.get(shareholder).numberQueue.put(currentNumber);
                    j++;
                }

                while (localNumberMap.size() != share.getNumberOfShareholders()){
                    Thread.sleep(1000);
                }
                BigInteger newNumber = BigInteger.ZERO;
                for (BigInteger number : localNumberMap.values()){
                    newNumber = newNumber.add(number);
                }
                newNumber = newNumber.mod(share.getModulus());

                destinationFile.write(fixLength(newNumber.toByteArray(), SHARE_SIZE));
            }

            for (ShareHolder shareholder: shareHolderList) {
                sslConnectionMap.get(shareholder).numberQueue.put(-1L);
            }

            new File(share.getName()).delete();
            new File(share.getName()+"_new").renameTo(new File(share.getName()));

            share.setRenewStatus("renewed");
            share.setLastRenewed(System.currentTimeMillis());
            dbSemaphore.acquire();
            sharesDao.update(share);
            dbSemaphore.release();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
