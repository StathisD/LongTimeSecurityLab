package de.tu_darmstadt;

/**
 * Created by Stathis on 6/8/17.
 */

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class ServerListener extends SSLConnection implements Runnable{

    private int port;
    int xValue;
    static ExecutorService pool;

    public final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    private static SSLServerSocketFactory sslServerSocketFactory;

    private ServerListener(int port){
        this.port = port;
    }

    static void startListeningForConnections() {
        System.setProperty("javax.net.ssl.keyStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
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

            boolean verifiability = in.readBoolean();

            ShareHolder[] shareHolders = (ShareHolder[]) in.readObject();


            initializeParameters(shareFileSize,1, verifiability);

            int numbersInFile = (int) Math.ceil(shareFileSize * 1.0 / MOD_SIZE);

            dbSemaphore.acquire();
            Share share = new Share(fileName + xValue, xValue, socket.getInetAddress().toString(), socket.getPort(), MODULUS, 5,3, numbersInFile);
            int j = 1;
            /*for (ShareHolder s : shareHolders){
                ShareHolder shareholder = shareholdersDao.queryForId(s.getIpAddress());
                ManyToMany manyToMany = new ManyToMany(shareholder, share, BigInteger.valueOf(j));
                manyToManyDao.createIfNotExists(manyToMany);
                j++;
            }*/

            sharesDao.createIfNotExists(share);
            dbSemaphore.release();

            RandomAccessFile shareFile = new RandomAccessFile(fileName + xValue, "rw");
            shareFile.seek(0L);

            boolean  verified = true;

            if (VERIFIABILITY){
                //verify
                ExecutorService pool = Executors.newFixedThreadPool(THREADS);
                Future[] futures = new Future[THREADS];
                int numberOfThreads = 0;
                long destStartingByte = 0;

                while (numbersReceived < numbersInFile && verified) {

                    for (int i = 0; i < THREADS; i++) {
                        BigInteger[] buffer = (BigInteger[]) in.readObject();
                        int numbers = buffer.length / (NEEDED_SHARES+2);
                        VerificationTask task = new VerificationTask(share.getNeededShares(), fileName, xValue, buffer, destStartingByte);
                        futures[i] = pool.submit(task);
                        destStartingByte += numbers*MOD_SIZE;
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
                        shareFile.write(fixLength(aBuffer.toByteArray(), MOD_SIZE));
                    }

                    numbersReceived += buffer.length;
                }
            }




            if ( shareFileSize == shareFile.length() && verified){
                show("Share File " + (xValue-1) + " created successfully");
                out.writeInt(0);
            }else{
                show("ERROR: Share File " + (xValue-1) + " does not have the correct size");
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

            dbSemaphore.acquire();
            Share share = sharesDao.queryForId(fileName + port % 8000);
            dbSemaphore.release();

            xValue = share.getxValue();


            RandomAccessFile shareFile = new RandomAccessFile(fileName + port % 8000, "r");
            shareFile.seek(0L);

            long shareFileSize = shareFile.length();

            initializeParameters( shareFileSize, 1,false);

            out.writeInt(xValue);
            out.flush();

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
                show("Share File " + (xValue-1) + " sent successfully");
                out.writeInt(0);
            }else{
                show("ERROR: Share File " + (xValue-1) + " could not be sent");
                out.writeInt(1);
            }
            out.flush();

        }catch(Exception e){
            Logger.getLogger(ServerListener.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void renewShare(){
        try{
            Share receivedShare = (Share) in.readObject();
            dbSemaphore.acquire();
            Share localShare = sharesDao.queryForId(receivedShare.getName());
            dbSemaphore.release();

            if (Math.abs(localShare.getLastRenewed()-System.currentTimeMillis()) >= (timeSlot-1000*60*60*24) && !localShare.getRenewStatus().equals("in_progress")){
                localShare.setRenewStatus("in_progress");
                dbSemaphore.acquire();
                sharesDao.update(localShare);
                dbSemaphore.release();
                out.writeInt(0);
                out.flush();
            }else{
                out.writeInt(1);
                out.flush();
                return;
            }

            String ipAddress = socket.getInetAddress().toString();
            dbSemaphore.acquire();
            ShareHolder shareHolder = shareholdersDao.queryForId(ipAddress);
            dbSemaphore.release();
            RenewShareTask.sslConnectionMap.put(shareHolder, this);

            new RenewShareTask(localShare).start();

            long currentNumber;
            while ((currentNumber = numberQueue.poll(10, TimeUnit.MINUTES)) == in.readLong()) {
                BigInteger localNumber = (BigInteger) in.readObject();
                RenewShareTask.localNumberMap.put(shareHolder, localNumber);
                out.writeObject(RenewShareTask.remoteNumberMap.get(shareHolder));
                out.flush();
            }






        }catch(Exception e){
            e.printStackTrace();
        }
    }

}