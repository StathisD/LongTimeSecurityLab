package de.tu_darmstadt;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
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

    private Share share;

    public RenewShareTask(Share share){
        this.share = share;
    }

    public void run(){
        // check share status and update it
        try{
            if (share.getRenewStatus().equals("needs renewal")){
                active = true;
                share.setRenewStatus("in progress");
                dbSemaphore.acquire();
                sharesDao.update(share);
                dbSemaphore.release();
            }else return;

            // initialize TLS sockets
            ExecutorService connectionPool = SSLClient.prepareConnections();

            dbSemaphore.acquire();
            // find other Shareholders of share
            List<ShareHolder> shareHolderList = lookupShareHoldersForShare(share);

            for (ShareHolder shareholder: shareHolderList) {
                // their x Values
                shareholder.setxValue(lookupXvalueForShareHolderAndShare(shareholder,share));

                // initialize communication with them
                if (!sslConnectionMap.containsKey(shareholder.getName())){
                    SSLClient sslClient = new SSLClient(shareholder, share);
                    sslConnectionMap.put(shareholder.getName(), sslClient);
                    connectionPool.submit(sslClient);
                }
            }
            dbSemaphore.release();

            // add own data to the list
            ShareHolder localShareholder = new ShareHolder(SERVER_NAME,"localhost",0);
            localShareholder.setxValue(BigInteger.valueOf(share.getxValue()));
            shareHolderList.add(localShareholder);

            // open share file
            RandomAccessFile sourceFile = new RandomAccessFile(SHARE_DIR + share.getName(), "r");
            RandomAccessFile destinationFile = new RandomAccessFile(SHARE_DIR + share.getName()+"_new", "rw");
            sourceFile.seek(0L);
            destinationFile.seek(0L);
            long targetFileSize = sourceFile.length();

            // verifiability disabled per default
            initializeParameters(targetFileSize, 0, false);

            long numbersInShare = targetFileSize / SHARE_SIZE;

            int maxNumbersInBuffer = BUFFER_SIZE / SHARE_SIZE;

            long numbersRenewed = 0;

            ExecutorService pool = Executors.newFixedThreadPool(THREADS);
            Future[] futures = new Future[THREADS];
            int numberOfThreads = 0;
            long[] currentNumbers = new long[THREADS];

            // proccess file in Threads and chunks, same principle as with Dealer program
            // create local data
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

                // send local data to other Shareholders and gather their data
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

                    // wait until all necessary data is gathered
                    while (localNumberMap.size() != share.getNumberOfShareholders()){
                        Thread.sleep(1000);
                    }

                    // check verification status
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

                    // create new share
                    byte[] buffer = new byte[numbersInBuffer*SHARE_SIZE];
                    sourceFile.readFully(buffer);
                    byte[] numberBytes = new byte[SHARE_SIZE];
                    for(int k=0; k<numbersInBuffer; k++){
                        System.arraycopy(buffer,k*SHARE_SIZE, numberBytes,0, SHARE_SIZE);
                        BigInteger newNumber  = new BigInteger(1, numberBytes);

                        for (ShareHolder shareholder: shareHolderList){
                            newNumber = newNumber.add(localNumberMap.get(shareholder.getName())[k]);
                        }
                        newNumber = newNumber.mod(MODULUS);
                        destinationFile.write(fixLength(newNumber.toByteArray(), SHARE_SIZE));
                    }
                }
            }

            // shutdown sockets
            for (ShareHolder shareholder: shareHolderList) {
                if (!shareholder.getName().equals(SERVER_NAME)){
                    sslConnectionMap.get(shareholder.getName()).numberQueue.put(-1L);
                }
            }

            // delete old share
            new File(SHARE_DIR + share.getName()).delete();
            new File(SHARE_DIR + share.getName()+"_new").renameTo(new File(SHARE_DIR + share.getName()));


            // update status and last Renewed date
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
