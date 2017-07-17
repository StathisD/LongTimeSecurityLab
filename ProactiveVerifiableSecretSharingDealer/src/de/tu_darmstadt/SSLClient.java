package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class SSLClient implements Runnable{
    private static SSLSocketFactory sslSocketFactory;
    public final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    int xValue;
    private ShareHolder shareHolder;
    private int mode;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private SSLClient(ShareHolder shareHolder, int mode){
        this.shareHolder = shareHolder;
        this.mode = mode;
    }

    static ExecutorService openNewConnections(int mode) {
        try {
            System.setProperty("javax.net.ssl.trustStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            ExecutorService pool = Executors.newCachedThreadPool();
            int numberThreads;
            switch (mode){
                case 1:
                    //Encryption
                    numberThreads = SHAREHOLDERS;
                    break;
                case 2:
                    //Decryption
                    numberThreads = NEEDED_SHARES;
                    break;
                default:
                    numberThreads = 0;
            }
            sslClients = new SSLClient[numberThreads];
            for (int i = 0; i < numberThreads; i++) {
                SSLClient sslClient = new SSLClient(shareHolders[i], mode);
                sslClients[i] = sslClient;
                pool.submit(sslClient);
            }
            pool.shutdown();
            return pool;
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
            return null;
        }
    }

    @Override
    public void run() {
        try{
            socket = sslSocketFactory.createSocket("localhost", shareHolder.getPort());
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeInt(mode);
            out.flush();

            switch (mode){
                case 1:
                    //Encryption
                    sendShares();
                    break;
                case 2:
                    //Decryption
                    receiveShares();
            }

            socket.close();
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }

    private void sendShares(){
        try{
            long dataSent = 0;
            while (xValue == 0){
                Thread.sleep(100);
            }

            out.writeObject(FILE_PATH);
            out.writeLong(SHARES_FILE_SIZE);
            out.writeInt(xValue);

            out.writeObject(MODULUS);

            out.writeBoolean(VERIFIABILITY);

            out.writeObject(shareHolders);
            out.flush();

            if (VERIFIABILITY) {
                out.writeObject(BigIntegerPolynomial.g);
                out.writeObject(BigIntegerPolynomial.h);
                out.flush();
            }

            while (dataSent != SHARES_FILE_SIZE) {
                byte[] buffer = queue.poll(10, TimeUnit.MINUTES);
                out.writeObject(buffer);
                //show("Socket " + (port % 8000) + " sent " + buffer.length + " data");
                dataSent += buffer.length;
                out.flush();
            }

            int result = in.readInt();
            if (result == 0){
                show("StoredFile File " + (xValue) + " sent successfully");
            }else{
                show("ERROR: StoredFile File " + (xValue) + " could not be sent");
            }
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }

    private void receiveShares(){
        try{
            long dataReceived = 0;

            out.writeObject(FILE_PATH);

            xValue = in.readInt();

            while (dataReceived != SHARES_FILE_SIZE) {
                byte[] buffer = (byte[]) in.readObject();
                //show("Socket " + (port%8000) + " received " + buffer.length + " data");
                queue.put(buffer);
                dataReceived += buffer.length;
            }

            int status = in.readInt();
            if (dataReceived == SHARES_FILE_SIZE && status == 0) {
                show("StoredFile File " + (xValue) + " received successfully");

            }else{
                show("ERROR: StoredFile File " + (xValue) + " could not be received");

            }

        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }



}