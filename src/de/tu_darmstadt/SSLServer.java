package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.ssl.SSLServerSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.BUFFER_SIZE;

public class SSLServer {

    static final int port = 8000;

    public static void startServer() {
        System.setProperty("javax.net.ssl.keyStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        SSLServerSocketFactory sslServerSocketFactory =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

        try {
            ServerSocket sslServerSocket = sslServerSocketFactory.createServerSocket(port);
            System.out.println("SSL ServerSocket started");
            System.out.println(sslServerSocket.toString());
            System.out.println("Waiting for incoming transmissions");
            Socket socket = sslServerSocket.accept();
            System.out.println("ServerSocket accepted");

            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);
            BufferedInputStream in = new BufferedInputStream((socket.getInputStream()), BUFFER_SIZE);
            while (true) {
                in.read();
            }

            //System.out.println("Closed");

        } catch (IOException ex) {
            Logger.getLogger(SSLServer.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

}