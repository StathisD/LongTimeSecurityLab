package de.tu_darmstadt;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Stathis on 7/2/17.
 */
public abstract class SSLConnection {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    public final LinkedBlockingQueue<Long> numberQueue = new LinkedBlockingQueue<>();
}
