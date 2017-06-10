package de.tu_darmstadt;

/**
 * Created by Stathis on 6/8/17.
 */

import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class SSLShareHolder implements Runnable{

    private int port;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private static SSLServerSocketFactory sslServerSocketFactory;

    private SSLShareHolder(int port){
        this.port = port;
    }

    static void startListeningForConnections() {
        System.setProperty("javax.net.ssl.keyStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ExecutorService pool = Executors.newCachedThreadPool();
        for (int port : ports) {
            SSLShareHolder sslShareHolder = new SSLShareHolder(port);
            pool.submit(sslShareHolder);
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

                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());

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
                }

                socket.close();
            }

            //sslServerSocket.close();

            //show("SeverSocket closed on port: " + port);
        }catch(Exception e){
            Logger.getLogger(SSLShareHolder.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void receiveShare(){
        try{
            long dataReceived = 0;

            long shareFileSize = in.readLong();
            initializeParameters( 4, shareFileSize);
            int xValue = in.readInt();
            int modSize = in.readInt();
            byte[] bytes = new byte[modSize];
            in.read(bytes);

            BigInteger modulus = new BigInteger(1, bytes);
            setMODULUS(modulus);

            RandomAccessFile shareFile = new RandomAccessFile(FILE_PATH + (xValue-1), "rw");
            shareFile.seek(0L);
            shareFile.writeInt(xValue);

            while (dataReceived != SHARES_FILE_SIZE_WITHOUT_HEADER) {
                byte[] buffer = new byte[1024*1024*16];
                int bytesRead = in.read(buffer);
                //show("Socket " + (port%8000) + " received " + bytesRead + " data");
                shareFile.write(buffer, 0, bytesRead);
                dataReceived += bytesRead;
            }

            if (shareFile.length() == SHARES_FILE_SIZE_WITH_HEADER){
                show("Share File " + (xValue-1) + " created successfully");
                out.writeInt(0);
            }else{
                show("ERROR: Share File " + (xValue-1) + " does not have the correct size");
                out.writeInt(1);
            }

        }catch(Exception e){
            Logger.getLogger(SSLShareHolder.class.getName())
                    .log(Level.SEVERE, null, e);
        }
    }

    private void sendShare(){
        try{
            RandomAccessFile shareFile = new RandomAccessFile(FILE_PATH + ((port%8000)-1), "r");
            shareFile.seek(0L);

            long shareFileSize = shareFile.length() - 4;

            initializeParameters(4, shareFileSize);

            int xValue = shareFile.readInt();
            out.writeInt(xValue);


            long dataSent = HEADER_LENGTH;

            while (dataSent < SHARES_FILE_SIZE_WITH_HEADER) {
                int bufferSize = (int) Math.min(16*1024,SHARES_FILE_SIZE_WITH_HEADER-dataSent);
                byte[] buffer = new byte[bufferSize];
                shareFile.readFully(buffer);
                out.write(buffer);
                //show("Socket " + (port%8000) + " sent " + buffer.length + " data");
                dataSent += buffer.length;

            }

            if (dataSent == SHARES_FILE_SIZE_WITH_HEADER){
                show("Share File " + (xValue-1) + " sent successfully");
                out.writeInt(0);
            }else{
                show("ERROR: Share File " + (xValue-1) + " could not be sent");
                out.writeInt(1);
            }

        }catch(Exception e){
            Logger.getLogger(SSLShareHolder.class.getName())
                    .log(Level.SEVERE, null, e);
        }

    }

}