package de.tu_darmstadt;

/**
 * Created by Stathis on 6/8/17.
 */

import com.j256.ormlite.dao.CloseableIterator;

import javax.net.ServerSocketFactory;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Database.lookupXvalueForShareHolderAndShare;
import static de.tu_darmstadt.Parameters.*;

public class ServerListener extends SSLConnection implements Runnable{

    static ExecutorService pool;
    private static /*SSL*/ServerSocketFactory sslServerSocketFactory;
    public final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private int port;
    int xValue;

    private ServerListener(int port){
        this.port = port;
    }

    static void startListeningForConnections() {
        //System.setProperty("javax.net.ssl.keyStore", "/media/stathis/9AEA2384EA235BAF/"+ SERVER_NAME + "/" + SERVER_NAME + "_keystore.jks");
        //System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        sslServerSocketFactory = /*(SSLServerSocketFactory) SSL*/ServerSocketFactory.getDefault();
        pool = Executors.newCachedThreadPool();
        for (int port : ports) {
            ServerListener serverListener = new ServerListener(port);
            pool.submit(serverListener);
        }
        pool.shutdown();
    }

    @Override
    public void run() {
        try{
            ServerSocket sslServerSocket = sslServerSocketFactory.createServerSocket(port);
            show("SSL ServerSocket started with: " +sslServerSocket.toString() +"\n" +"Waiting for incoming transmissions");

            while(true){
                socket = sslServerSocket.accept();
                show("ServerSocket accepted on port: " + port);
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                int mode = in.readInt();

                switch (mode){
                    case 1:
                        //Encryption
                        receiveShare();
                        break;
                    case 2:
                        //Decryption
                        sendShare();
                        break;
                    case 3:
                        //Decryption
                        deleteShare();
                        break;
                    case 4:
                        //Renewal
                        renewShare();
                        break;
                }

                socket.close();
            }

            //sslServerSocket.close();

            //show("SeverSocket closed on port: " + port);
        }catch(Exception e){
            Logger.getLogger(ServerListener.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void receiveShare(){
        try{
            long numbersReceived = 0;

            String fileName = (String) in.readObject();

            long shareFileSize = in.readLong();

            xValue = in.readInt();

            int neededShares = in.readInt();

            boolean verifiability = in.readBoolean();

            ShareHolder[] shareHolders = (ShareHolder[]) in.readObject();

            initializeParameters(shareFileSize,1, verifiability);

            int numbersInFile = (int) Math.ceil(shareFileSize * 1.0 / SHARE_SIZE);

            dbSemaphore.acquire();
            Share share = new Share(fileName, xValue, socket.getInetAddress().toString(), socket.getPort(), MODULUS, shareHolders.length, neededShares, numbersInFile);

            for (ShareHolder s : shareHolders){
                if (!s.getName().equals(SERVER_NAME)){
                    ShareHolder shareholder = shareholdersDao.queryForId(s.getName());
                    if (shareholder == null) {
                        shareholdersDao.create(s);
                    }
                    ManyToMany manyToMany = new ManyToMany(shareholder, share, s.getxValue());
                    manyToManyDao.createIfNotExists(manyToMany);
                }

            }

            sharesDao.createIfNotExists(share);
            dbSemaphore.release();

            RandomAccessFile shareFile = new RandomAccessFile(SHARE_DIR + fileName, "rw");
            shareFile.seek(0L);

            boolean  verified = true;

            if (VERIFIABILITY){
                //verify
                numbersInFile = numbersInFile * (neededShares + 2);
                ExecutorService pool = Executors.newFixedThreadPool(THREADS);
                Future[] futures = new Future[THREADS];
                int numberOfThreads = 0;
                long destStartingByte = 0;

                while (numbersReceived < numbersInFile && verified) {

                    for (int i = 0; i < THREADS; i++) {

                        BigInteger[] buffer = (BigInteger[]) in.readObject();

                        int numbers = buffer.length / (neededShares + 2);

                        VerificationTask task = new VerificationTask(neededShares, fileName, xValue, buffer, destStartingByte);
                        futures[i] = pool.submit(task);
                        destStartingByte += numbers * SHARE_SIZE;
                        numbersReceived += buffer.length;
                        numberOfThreads = i;
                        if (numbersReceived >= numbersInFile) break;
                    }

                    for (int i = 0; i <= numberOfThreads; i++) {
                        int returnValue = (int) futures[i].get();
                        if (returnValue != 0){
                            show("Verification Error");
                            pool.shutdownNow();
                            while (!pool.isTerminated()){
                                Thread.sleep(100);
                            }
                            new File(fileName + xValue).delete();
                            verified = false;
                            break;
                        }
                    }

                }
            }else{
                while (numbersReceived < numbersInFile) {
                    BigInteger[] buffer = (BigInteger[]) in.readObject();

                    for (BigInteger aBuffer : buffer) {
                        shareFile.write(fixLength(aBuffer.toByteArray(), SHARE_SIZE));
                    }

                    numbersReceived += buffer.length;
                }
            }

            if ( shareFileSize == shareFile.length() && verified){
                show("Share File " + xValue + " created successfully");
                out.writeInt(0);
            }else{
                show("ERROR: Share File " + xValue + " does not have the correct size");
                out.writeInt(1);
            }
            out.flush();

        }catch(Exception e){
            Logger.getLogger(ServerListener.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void sendShare(){
        try{
            String fileName = (String) in.readObject();

            RandomAccessFile shareFile = new RandomAccessFile(SHARE_DIR + fileName, "r");
            shareFile.seek(0L);

            long shareFileSize = shareFile.length();

            initializeParameters( shareFileSize, 1,false);

            long dataSent = 0;

            while (dataSent < shareFileSize) {
                int bufferSize = (int) Math.min(BUFFER_SIZE, shareFileSize-dataSent);
                byte[] buffer = new byte[bufferSize];
                shareFile.readFully(buffer);
                out.writeObject(buffer);
                out.flush();
                //show("Socket " + (port%8000) + " sent " + buffer.length + " data");
                dataSent += buffer.length;
            }

            if (dataSent == shareFileSize){
                show("Share File " + SERVER_NAME + " sent successfully");
                out.writeInt(0);
                new File(SHARE_DIR + fileName).delete();
                dbSemaphore.acquire();
                sharesDao.deleteById(fileName);
                CloseableIterator<ManyToMany> iterator = manyToManyDao.closeableIterator();
                try {
                    while (iterator.hasNext()) {
                        ManyToMany manyToMany = iterator.next();
                        if (manyToMany.share.getName().equals(fileName)){
                            manyToManyDao.delete(manyToMany);
                        }
                    }
                } finally {
                    // close it at the end to close underlying SQL statement
                    iterator.close();
                }
                dbSemaphore.release();
            }else{
                show("ERROR: Share File " + SERVER_NAME + " could not be sent");
                out.writeInt(1);
            }
            out.flush();

        }catch(Exception e){
            Logger.getLogger(ServerListener.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void deleteShare(){
        try{
            String fileName = (String) in.readObject();
            new File(SHARE_DIR + fileName).delete();
            dbSemaphore.acquire();
            sharesDao.deleteById(fileName);
            CloseableIterator<ManyToMany> iterator = manyToManyDao.closeableIterator();
            try {
                while (iterator.hasNext()) {
                    ManyToMany manyToMany = iterator.next();
                    if (manyToMany.share.getName().equals(fileName)){
                        manyToManyDao.delete(manyToMany);
                    }
                }
            } finally {
                // close it at the end to close underlying SQL statement
                iterator.close();
            }
            dbSemaphore.release();

            show("Share File " + SERVER_NAME + " deleted successfully");
            out.writeInt(0);
            out.flush();

        }catch(Exception e){
            Logger.getLogger(ServerListener.class.getName())
                    .log(Level.SEVERE, null, e);
            show("ERROR: Share File " + SERVER_NAME + " could not be deleted");
            try{
                out.writeInt(1);
                out.flush();
            }catch(Exception ed){
                Logger.getLogger(ServerListener.class.getName())
                        .log(Level.SEVERE, null, ed);
            }
        }
    }


    private void renewShare(){
        try{
            String remoteServerName = (String) in.readObject();
            String shareName = (String) in.readObject();
            dbSemaphore.acquire();
            Share localShare = sharesDao.queryForId(shareName);
            dbSemaphore.release();

            if (localShare.getRenewStatus().equals("needs renewal")){
                out.writeInt(0);
                out.flush();
            }else{
                out.writeInt(1);
                out.flush();
                return;
            }

            dbSemaphore.acquire();
            ShareHolder shareHolder = shareholdersDao.queryForId(remoteServerName);
            shareHolder.setxValue(lookupXvalueForShareHolderAndShare(shareHolder,localShare));
            dbSemaphore.release();
            RenewShareTask.sslConnectionMap.put(shareHolder.getName(), this);
            show(shareHolder.getxValue());
            long backoffTime = (long) (new Random().nextFloat() * (1000 * 20));
            Thread.sleep(backoffTime);

            if (!RenewShareTask.active) {
                new RenewShareTask(localShare).start();
            }

            long remoteCurrentNumber = in.readLong();
            show("Remote current number received " + remoteCurrentNumber);
            long currentNumber = numberQueue.poll(10, TimeUnit.MINUTES);
            show("Local current number " + currentNumber);
            out.writeLong(currentNumber);
            out.flush();

            while (currentNumber == remoteCurrentNumber && currentNumber >=0) {
                BigInteger[] localNumberArray = (BigInteger[]) in.readObject();
                if (VERIFIABILITY){
                    localNumberArray = new ProactiveVerificationTask(localShare.getNeededShares(), localShare.getxValue(),localNumberArray).call();
                    if (localNumberArray == null){
                        RenewShareTask.verificationSuccess = false;
                    }
                }
                RenewShareTask.localNumberMap.put(shareHolder.getName(), localNumberArray);
                out.writeObject(RenewShareTask.remoteNumberMap.get(shareHolder.getName()));
                out.flush();
                remoteCurrentNumber = in.readLong();
                currentNumber = numberQueue.poll(10, TimeUnit.MINUTES);
                out.writeLong(currentNumber);
                out.flush();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}