package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Parameters.THREADS;
import static de.tu_darmstadt.Parameters.show;


/**
 * Created by Stathis on 6/11/17.
 */

public class ProactiveVerificationTask {
    private BigInteger[] buffer;
    private int xValue;
    private int neededShares;

    public ProactiveVerificationTask(int neededShares, int xValue, BigInteger[] buffer) {
        this.buffer = buffer;
        this.xValue = xValue;
        this.neededShares = neededShares;
    }

    public BigInteger[] call() {

        try {
            ExecutorService pool = Executors.newFixedThreadPool(THREADS);


            int taskSize = buffer.length / THREADS;
            taskSize = taskSize - taskSize % (neededShares + 2);
            int threads;
            if (taskSize * THREADS < buffer.length) {
                threads = THREADS + 1;
            } else {
                threads = THREADS;
            }
            Future[] futures = new Future[threads];

            int numbersVerified = 0;
            for (int i = 0; i < threads; i++) {
                BigInteger[] data;
                if (numbersVerified + taskSize <= buffer.length) {
                    data = new BigInteger[taskSize];
                } else {
                    data = new BigInteger[buffer.length - numbersVerified];
                }
                System.arraycopy(buffer, numbersVerified, data, 0, data.length);
                ProactiveVerifcationCompletionTask task = new ProactiveVerifcationCompletionTask(neededShares, xValue, data);
                futures[i] = pool.submit(task);
                numbersVerified += data.length;
            }
            BigInteger[] numbers = new BigInteger[buffer.length / (neededShares + 2)];
            int currentPos = 0;
            for (int i = 0; i < threads; i++) {
                BigInteger[] result = (BigInteger[]) futures[i].get();
                System.arraycopy(result, 0, numbers, currentPos, result.length);
                currentPos += result.length;
            }

            show("verification success");

            return numbers;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}