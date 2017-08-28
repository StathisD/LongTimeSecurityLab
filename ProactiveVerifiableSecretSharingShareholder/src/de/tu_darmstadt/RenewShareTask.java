package de.tu_darmstadt;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Database.lookupShareHoldersForShare;
import static de.tu_darmstadt.Database.lookupXvalueForShareHolderAndShare;
import static de.tu_darmstadt.Parameters.*;

/**
 * Created by Stathis on 7/1/17.
 */
public class RenewShareTask extends Thread{

    public static HashMap<String, BigInteger[]> remoteNumberMap = new HashMap<>();

    public static HashMap<String, BigInteger[]> localNumberMap = new HashMap<>();

    public static HashMap<String, SSLConnection> sslConnectionMap = new HashMap<>();

    public static long currentNumber;

    public static boolean active = false;

    public static boolean verificationSuccess = true;

    public static int N = 8000;

    private Share share;

    public RenewShareTask(Share share){
        this.share = share;
    }

    public void run(){
        try{
            if (share.getRenewStatus().equals("needs renewal")){
                active = true;
                share.setRenewStatus("in progress");
                dbSemaphore.acquire();
                sharesDao.update(share);
                dbSemaphore.release();
            }else return;

            ExecutorService connectionPool = SSLClient.prepareConnections();

            dbSemaphore.acquire();
            List<ShareHolder> shareHolderList = lookupShareHoldersForShare(share);

            for (ShareHolder shareholder: shareHolderList) {

                shareholder.setxValue(lookupXvalueForShareHolderAndShare(shareholder,share));

                if (!sslConnectionMap.containsKey(shareholder.getName())){
                    SSLClient sslClient = new SSLClient(shareholder, share);
                    sslConnectionMap.put(shareholder.getName(), sslClient);
                    connectionPool.submit(sslClient);
                }
            }
            dbSemaphore.release();

            ShareHolder localShareholder = new ShareHolder(SERVER_NAME,"localhost",0);
            localShareholder.setxValue(BigInteger.valueOf(share.getxValue()));
            shareHolderList.add(localShareholder);

            RandomAccessFile sourceFile = new RandomAccessFile(SHARE_DIR + share.getName(), "r");
            RandomAccessFile destinationFile = new RandomAccessFile(SHARE_DIR + share.getName()+"_new", "rw");
            sourceFile.seek(0L);
            destinationFile.seek(0L);
            long targetFileSize = sourceFile.length();

            initializeParameters(targetFileSize, 0, true);

            long numbersInShare = targetFileSize / SHARE_SIZE;

            int maxNumbersInBuffer = BUFFER_SIZE / SHARE_SIZE;

            long numbersRenewed = 0;

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            Future[] futures = new Future[THREADS];
            int numberOfThreads = 0;
            long[] currentNumbers = new long[THREADS];

            while(numbersRenewed < numbersInShare) {

                int numbersInBuffer = 0;
                for (int i = 0; i < THREADS; i++) {
                    if (numbersInShare - numbersRenewed < maxNumbersInBuffer){
                        numbersInBuffer = (int) (numbersInShare - numbersRenewed);
                    }else{
                        numbersInBuffer = maxNumbersInBuffer;
                    }
                    ProactiveVerificationInitializationTask task = new ProactiveVerificationInitializationTask(numbersInBuffer, share, shareHolderList);
                    futures[i] = pool.submit(task);
                    currentNumbers[i] = numbersRenewed;
                    numbersRenewed += numbersInBuffer;
                    numberOfThreads = i;
                    if (numbersRenewed >= numbersInShare) break;
                }

                for (int i = 0; i <= numberOfThreads; i++) {
                    remoteNumberMap = new HashMap<>();
                    localNumberMap = new HashMap<>();
                    currentNumber = currentNumbers[i];
                    show("Renewing Number "+ currentNumber);
                    BigInteger[][] remoteNumbers = (BigInteger[][]) futures[i].get();
                    int j= 0;
                    for (ShareHolder shareholder: shareHolderList) {
                        if (!shareholder.getName().equals(SERVER_NAME)){
                            remoteNumberMap.put(shareholder.getName(), remoteNumbers[j]);
                            sslConnectionMap.get(shareholder.getName()).numberQueue.put(currentNumber);
                        }else{
                            localNumberMap.put(shareholder.getName(), remoteNumbers[j]);
                        }
                        j++;
                    }
                    while (localNumberMap.size() != share.getNumberOfShareholders()){
                        Thread.sleep(1000);
                    }
                    if(VERIFIABILITY && !verificationSuccess){
                        show("Error in Verification");
                        share.setRenewStatus("needs renewal");
                        dbSemaphore.acquire();
                        sharesDao.update(share);
                        dbSemaphore.release();
                        active = false;
                        verificationSuccess = true;
                        for (ShareHolder shareholder: shareHolderList) {
                            if (!shareholder.getName().equals(SERVER_NAME)){
                                sslConnectionMap.get(shareholder.getName()).numberQueue.put(-1L);
                            }
                        }
                        return;
                    }

                    byte[] buffer = new byte[numbersInBuffer*SHARE_SIZE];
                    sourceFile.readFully(buffer);
                    byte[] numberBytes = new byte[SHARE_SIZE];
                    for(int k=0; k<numbersInBuffer; k++){
                        System.arraycopy(buffer,k*SHARE_SIZE, numberBytes,0, SHARE_SIZE);
                        BigInteger newNumber  = new BigInteger(1, numberBytes);

                        for (ShareHolder shareholder: shareHolderList){
                            newNumber = newNumber.add(localNumberMap.get(shareholder.getName())[k]);
                            if (k==0) show (newNumber);
                        }
                        newNumber = newNumber.mod(MODULUS);
                        destinationFile.write(fixLength(newNumber.toByteArray(), SHARE_SIZE));
                    }
                }
            }

            for (ShareHolder shareholder: shareHolderList) {
                if (!shareholder.getName().equals(SERVER_NAME)){
                    sslConnectionMap.get(shareholder.getName()).numberQueue.put(-1L);
                }
            }

            new File(SHARE_DIR + share.getName()).delete();
            new File(SHARE_DIR + share.getName()+"_new").renameTo(new File(SHARE_DIR + share.getName()));

            share.setRenewStatus("renewed");
            share.setLastRenewed(System.currentTimeMillis());
            dbSemaphore.acquire();
            sharesDao.update(share);
            dbSemaphore.release();
            active = false;
            show("Renewal of Share " + share.getName() + " was completed successfully");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
