package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.Socket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;


public class SSLClient implements Runnable{
    private static String address = "localhost";
    private int port;
    private int mode;
    int xValue;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    public final LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();

    private static SSLSocketFactory sslSocketFactory;

    private SSLClient(int port, int mode){
        this.port = port;
        this.mode = mode;
    }

    static void openNewConnections(int mode) {
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
                sslClients[i] = new SSLClient(ports[i], mode);
                pool.submit(sslClients[i]);
            }
            pool.shutdown();
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }

    @Override
    public void run() {
        try{
            socket = sslSocketFactory.createSocket(address, port);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            out.writeInt(mode);

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
            out.writeLong(SHARES_FILE_SIZE_WITHOUT_HEADER);
            out.writeInt(port % 8000);
            out.writeInt(SHARE_SIZE);
            out.write(fixLength(MODULUS.toByteArray(), SHARE_SIZE));

            while (dataSent != SHARES_FILE_SIZE_WITHOUT_HEADER) {
                byte[] buffer = queue.poll(10, TimeUnit.MINUTES);
                out.write(buffer);
                //show("Socket " + (port%8000) + " sent " + buffer.length + " data");
                dataSent += buffer.length;
            }
            int result = in.readInt();
            if (result == 0){
                show("Share File " + (port % 8000) + " sent successfully");
            }else{
                show("ERROR: Share File " + (port % 8000) + " could not be sent");
            }
        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }

    private void receiveShares(){
        try{
            long dataReceived = 0;

            xValue = in.readInt();

            while (dataReceived != SHARES_FILE_SIZE_WITHOUT_HEADER) {
                int bufferSize = 0;
                int limit = (int) Math.min(BUFFER_SIZE, SHARES_FILE_SIZE_WITHOUT_HEADER - dataReceived );
                byte[] encryptedData = new byte[limit ];
                while(bufferSize < limit ){
                    byte[] buffer = new byte[limit  - bufferSize];
                    int bytesRead = in.read(buffer);
                    System.arraycopy(buffer, 0, encryptedData, bufferSize, bytesRead);
                    bufferSize += bytesRead;
                }
                if(encryptedData.length != BUFFER_SIZE) show(encryptedData.length);
                queue.put(encryptedData);
                dataReceived += bufferSize;
            }
            int status = in.readInt();
            if (dataReceived == SHARES_FILE_SIZE_WITHOUT_HEADER && status ==0){
                show("Share File " + (xValue-1) + " received successfully");

            }else{
                show("ERROR: Share File " + (xValue-1) + " could not be received");

            }

        } catch (Exception e) {
            Logger.getLogger(SSLClient.class.getName());
        }
    }
}