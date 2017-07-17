package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

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
    private static SSLSocketFactory sslSocketFactory;

    private ShareHolder shareHolder;
    private Share share;


    public SSLClient(ShareHolder shareHolder, Share share){
        this.shareHolder = shareHolder;
        this.share = share;
    }

    static ExecutorService prepareConnections() {
        try {
            System.setProperty("javax.net.ssl.trustStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
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
            for(int port : ports){
                try{
                    socket = sslSocketFactory.createSocket(shareHolder.getIpAddress(), port);
                    break;
                }catch (IOException e){}
            }
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            //send renew code
            out.writeInt(3);
            out.writeObject(share);
            out.flush();

            //wait for ack
            int ack = in.readInt();

            switch (ack){
                case 1:
                    // connection already established as a client
                    break;
                case 0:
                    //ready to renew
                    long currentNumber;
                    while ((currentNumber = numberQueue.poll(10, TimeUnit.MINUTES)) >= 0){
                        out.writeLong(currentNumber);
                        out.writeObject(RenewShareTask.remoteNumberMap.get(shareHolder));
                        out.flush();
                        BigInteger number = (BigInteger) in.readObject();
                        RenewShareTask.localNumberMap.put(shareHolder, number);
                    }
            }

            socket.close();
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }


}