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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.*;

public class ServerListener implements Runnable{

    private int port;
    private Socket socket;
    int xValue;
    private ObjectOutputStream out;
    private ObjectInputStream in;
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
            long dataReceived = 0;

            long shareFileSize = in.readLong();

            xValue = in.readInt();

            BigInteger modulus = (BigInteger) in.readObject();
            setMODULUS(modulus);

            boolean verifiability = in.readBoolean();

            ShareHolder[] shareHolders = (ShareHolder[]) in.readObject();

            if (verifiability)  {
                shareFileSize = shareFileSize/3;

                BigIntegerPolynomial.g = (BigInteger) in.readObject();

                BigIntegerPolynomial.h = (BigInteger) in.readObject();
            }

            initializeParameters( modulus.bitLength(), shareFileSize, verifiability);

            Share share = new Share("dummy"+xValue, xValue, socket.getInetAddress().toString(), socket.getPort(), modulus, 5,3);
            dbSemaphore.acquire();
            sharesDao.create(share);
            for (ShareHolder s : shareHolders){
                shareholdersDao.createIfNotExists(s);
                ManyToMany manyToMany = new ManyToMany(s, share);
                manyToManyDao.createIfNotExists(manyToMany);
            }
            dbSemaphore.release();
            RandomAccessFile shareFile = new RandomAccessFile(FILE_PATH + (xValue-1), "rw");
            shareFile.seek(0L);

            if (verifiability){
                Verifier verifier = new Verifier(this);
                verifier.start();
                while (dataReceived != SHARES_FILE_SIZE) {
                    int bufferSize = 0;
                    int limit = (int) Math.min(BUFFER_SIZE, SHARES_FILE_SIZE - dataReceived );
                    byte[] encryptedData = new byte[limit ];
                    while(bufferSize < limit ){
                        byte[] buffer = new byte[limit  - bufferSize];
                        int bytesRead = in.read(buffer);
                        show("Socket " + (port%8000) + " received " + bytesRead + " data");
                        if (bytesRead > 0){
                            shareFile.write(buffer, 0, bytesRead);
                            System.arraycopy(buffer, 0, encryptedData, bufferSize, bytesRead);
                            bufferSize += bytesRead;
                        }

                    }
                    if(encryptedData.length != BUFFER_SIZE) show(encryptedData.length);
                    queue.put(encryptedData);
                    dataReceived += bufferSize;
                }
            }else {
                while (dataReceived != SHARES_FILE_SIZE) {
                    byte[] buffer = (byte[]) in.readObject();
                    //show("Socket " + (port%8000) + " received " + buffer.length + " data");
                    shareFile.write(buffer);
                    dataReceived += buffer.length;
                }
            }

            if (shareFile.length() == SHARES_FILE_SIZE){
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
            dbSemaphore.acquire();
            Share share = sharesDao.queryForId("dummy"+(port%8000));
            dbSemaphore.release();

            xValue = share.getxValue();


            RandomAccessFile shareFile = new RandomAccessFile(FILE_PATH + ((port%8000)-1), "r");
            shareFile.seek(0L);

            long shareFileSize = shareFile.length();

            initializeParameters(share.getModulus().bitLength()/8, shareFileSize, false);

            out.writeInt(xValue);
            out.flush();

            long dataSent = 0;

            while (dataSent < SHARES_FILE_SIZE) {
                int bufferSize = (int) Math.min(BUFFER_SIZE, SHARES_FILE_SIZE-dataSent);
                byte[] buffer = new byte[bufferSize];
                shareFile.readFully(buffer);
                out.writeObject(buffer);
                out.flush();
                //show("Socket " + (port%8000) + " sent " + buffer.length + " data");
                dataSent += buffer.length;
            }

            if (dataSent == SHARES_FILE_SIZE){
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
        BigIntegerPolynomial d = new BigIntegerPolynomial(NEEDED_SHARES - 1, MODULUS, BigInteger.ZERO);
        for (int j = 0; j<SHAREHOLDERS; j++){
            BigInteger uj = d.evaluatePolynom(BigInteger.valueOf(j+1));
        }
    }

}