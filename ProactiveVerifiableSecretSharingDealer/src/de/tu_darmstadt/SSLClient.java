package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class SSLClient implements Runnable{
    private static SSLSocketFactory sslSocketFactory;
    public final LinkedBlockingQueue<BigInteger[]> numberQueue = new LinkedBlockingQueue<>();
    public final LinkedBlockingQueue<byte[]> byteQueue = new LinkedBlockingQueue<>();
    int xValue;
    private ShareHolder shareHolder;
    private int mode;
    private SSLSocket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    public String status;

    public SSLClient(ShareHolder shareHolder, int mode) {
        this.shareHolder = shareHolder;
        this.mode = mode;
        status = "starting";
    }

    static ExecutorService openNewConnections(int mode) {
        try {
            System.setProperty("javax.net.ssl.trustStore", "Client_keystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            sslSocketFactory = SSLContext.getDefault().getSocketFactory();
            ExecutorService pool = Executors.newCachedThreadPool();
            int numberThreads;
            switch (mode){
                case 1:
                    //Encryption
                    numberThreads = SHAREHOLDERS;
                    sslClients = new SSLClient[numberThreads];
                    for (int i = 0; i < numberThreads; i++) {
                        SSLClient sslClient = new SSLClient(shareHolders[i], mode);
                        sslClients[i] = sslClient;
                        pool.submit(sslClient);
                    }
                    pool.shutdown();
                    break;
            }
            return pool;
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            return null;
        }
    }

    @Override
    public void run() {
        try{
            socket = (SSLSocket) sslSocketFactory.createSocket("localhost", shareHolder.getPort());
            String[] suites = socket.getSupportedCipherSuites();
            socket.setEnabledCipherSuites(suites);
            socket.addHandshakeCompletedListener(new MyHandshakeListener());
            socket.startHandshake();
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeInt(mode);
            out.flush();

            status = "connected";
            switch (mode){
                case 1:
                    //Encryption
                    sendShares();
                    break;
                case 2:
                    //Decryption
                    receiveShares();
                    break;
                case 3:
                    //Decryption
                    deleteShare();
                    break;
            }

            socket.close();
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }

    private void sendShares(){
        try{
            long numbersSent = 0;
            while (xValue == 0){
                Thread.sleep(100);
            }

            out.writeObject(FILE_NAME);
            out.writeLong(SHARES_FILE_SIZE);
            out.writeInt(xValue);

            out.writeInt(NEEDED_SHARES);

            out.writeBoolean(VERIFIABILITY);

            out.writeObject(shareHolders);

            out.flush();

            int numbersInFile = (int) Math.ceil(TARGET_FILE_SIZE * 1.0 / BLOCK_SIZE);

            if (VERIFIABILITY) {
                numbersInFile = numbersInFile*(NEEDED_SHARES+2);
            }

            while (numbersSent < numbersInFile) {
                BigInteger[] buffer = numberQueue.poll(10, TimeUnit.MINUTES);
                out.writeObject(buffer);

                numbersSent += buffer.length;
                out.flush();
            }

            int result = in.readInt();
            if (result == 0){
                show("StoredFile File " + (xValue) + " sent successfully");
                status = "successful";
            }else{
                show("ERROR: StoredFile File " + (xValue) + " could not be sent");
                status = "unsuccessful";
            }
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            status = "unsuccessful";
        }
    }

    private void receiveShares(){
        try{
            long dataReceived = 0;

            out.writeObject(FILE_NAME);

            out.flush();

            while (dataReceived != SHARES_FILE_SIZE) {
                byte[] buffer = (byte[]) in.readObject();
                //show("Socket " + (port%8000) + " received " + buffer.length + " data");
                byteQueue.put(buffer);
                dataReceived += buffer.length;
            }

            int result = in.readInt();
            if (dataReceived == SHARES_FILE_SIZE && result == 0) {
                show("StoredFile File " + (xValue) + " received successfully");
                status = "successful";
            }else{
                show("ERROR: StoredFile File " + (xValue) + " could not be received");
                status = "unsuccessful";
            }

        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            status = "unsuccessful";
        }
    }

    private void deleteShare() {
        try {
            out.writeObject(FILE_NAME);

            out.flush();

            int result = in.readInt();
            if (result == 0) {
                show("Remote File Share deleted successfully");
                status = "successful";
            } else {
                show("ERROR: Remote File Share could not be deleted");
                status = "unsuccessful";
            }

        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            status = "unsuccessful";
        }
    }
}

class MyHandshakeListener implements HandshakeCompletedListener {
    public void handshakeCompleted(HandshakeCompletedEvent e) {
        System.out.println("Handshake successful!");
        System.out.println("Using cipher suite: " + e.getCipherSuite());
    }
}