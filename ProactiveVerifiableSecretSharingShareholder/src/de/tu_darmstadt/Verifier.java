package de.tu_darmstadt;

import java.util.concurrent.*;

import static de.tu_darmstadt.Parameters.THREADS;
import static de.tu_darmstadt.Parameters.VERIFICATION_FILE_SIZE;
import static de.tu_darmstadt.Parameters.show;

/**
 * Created by Stathis on 6/11/17.
 */
public class Verifier extends Thread{
    ServerListener serverListener;

    public Verifier(ServerListener serverListener){
        this.serverListener = serverListener;
    }

    public void run(){
        try {
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);

            long processed = 0;
            int numberOfThreads = 0;
            byte buffer[];
            Future[] futures = new Future[THREADS];

            while (processed < VERIFICATION_FILE_SIZE) {

                for (int i = 0; i < THREADS; i++) {

                    buffer = serverListener.queue.poll(10, TimeUnit.MINUTES);

                    VerificationTask task = new VerificationTask(buffer, serverListener.xValue);
                    futures[i]= pool.submit(task);

                    processed += buffer.length;
                    numberOfThreads = i;
                    if (processed >= VERIFICATION_FILE_SIZE) break;
                }

                for (int i = 0; i <= numberOfThreads; i++) {
                    int returnValue = (int) futures[i].get();
                    if (returnValue != 0) show("An invalid share was found");
                }
            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);
            show("Verification Completed");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
