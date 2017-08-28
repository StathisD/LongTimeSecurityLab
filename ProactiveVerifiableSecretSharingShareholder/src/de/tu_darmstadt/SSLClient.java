package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class SSLClient extends SSLConnection implements Runnable{
    private static /*SSL*/SocketFactory sslSocketFactory;

    private ShareHolder shareHolder;
    private Share share;


    public SSLClient(ShareHolder shareHolder, Share share){
        this.shareHolder = shareHolder;
        this.share = share;
    }

    static ExecutorService prepareConnections() {
        try {
            //System.setProperty("javax.net.ssl.trustStore", "/media/stathis/9AEA2384EA235BAF/"+ SERVER_NAME + "/" + SERVER_NAME + "_keystore.jks");
            //System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            sslSocketFactory = /*(SSLSocketFactory) SSL*/SocketFactory.getDefault();
            ExecutorService pool = Executors.newCachedThreadPool();
            return pool;
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            return null;
        }
    }

    @Override
    public void run() {
        try{
            socket = sslSocketFactory.createSocket(shareHolder.getIpAddress(), shareHolder.getPort());

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            //send renew code
            out.writeInt(4);
            out.writeObject(SERVER_NAME);
            out.writeObject(share.getName());
            out.flush();

            //wait for ack
            int ack = in.readInt();

            switch (ack){
                case 1:
                    // connection already established as a client
                    break;
                case 0:
                    //ready to renew
                    long currentNumber = numberQueue.poll(10, TimeUnit.MINUTES);
                    show("Local current number " + currentNumber);
                    out.writeLong(currentNumber);
                    out.flush();
                    long remoteCurrentNumber = in.readLong();
                    show("Remote current number received " + remoteCurrentNumber);
                    while (currentNumber  >= 0 && remoteCurrentNumber == currentNumber){
                        out.writeObject(RenewShareTask.remoteNumberMap.get(shareHolder.getName()));
                        show("Send array for " + currentNumber);
                        out.flush();
                        BigInteger[] localNumberArray = (BigInteger[]) in.readObject();
                        if (VERIFIABILITY){
                            localNumberArray = new ProactiveVerificationTask(share.getNeededShares(), share.getxValue(),localNumberArray).call();
                            if (localNumberArray == null){
                                RenewShareTask.verificationSuccess = false;
                            }
                        }
                        show("received array for " + currentNumber);
                        RenewShareTask.localNumberMap.put(shareHolder.getName(), localNumberArray);
                        currentNumber = numberQueue.poll(10, TimeUnit.MINUTES);
                        show("Local current number " + currentNumber);
                        out.writeLong(currentNumber);
                        out.flush();
                        remoteCurrentNumber = in.readLong();
                        show("Remote current number received " + remoteCurrentNumber);
                    }
            }

            socket.close();
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }


}