package de.tu_darmstadt;

import javax.net.ssl.SSLSocket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Stathis on 7/2/17.
 */
public abstract class SSLConnection {
    SSLSocket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    public final LinkedBlockingQueue<Long> numberQueue = new LinkedBlockingQueue<>();
}
