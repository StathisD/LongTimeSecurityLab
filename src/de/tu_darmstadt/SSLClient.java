package de.tu_darmstadt;

/**
 * Created by stathis on 6/8/17.
 */

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.tu_darmstadt.Parameters.BUFFER_SIZE;

public class SSLClient {

    static final int port = 8000;

    public static void startClient() {
        System.setProperty("javax.net.ssl.trustStore", "/media/stathis/9AEA2384EA235BAF/keystore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        SSLSocketFactory sslSocketFactory =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
        try {
            Socket socket = sslSocketFactory.createSocket("localhost", port);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);
            BufferedInputStream in = new BufferedInputStream((socket.getInputStream()), BUFFER_SIZE);
            while (true) {
                System.out.println("Enter something:");
                Scanner scanner = new Scanner(System.in);
                String inputLine = scanner.nextLine();
                if (inputLine.equals("q")) {
                    break;
                }
                out.write(inputLine.getBytes());
                byte[] buffer = new byte[10];
                in.read(buffer);
            }


        } catch (IOException ex) {
            Logger.getLogger(SSLClient.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

}