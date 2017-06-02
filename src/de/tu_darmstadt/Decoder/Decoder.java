package de.tu_darmstadt.Decoder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Constants.NBITS;

/**
 * Created by stathis on 6/3/17.
 */
public class Decoder {

    public static byte[] decode(ArrayList<BigInteger> encodedData) {
        ExecutorService pool = Executors.newCachedThreadPool();
        BigInteger[] array = encodedData.toArray(new BigInteger[encodedData.size()]);
        DecodingTask decodingTask = new DecodingTask(array, NBITS);
        Future<byte[]> future = pool.submit(decodingTask);
        try {
            byte[] result = future.get();
            pool.shutdown();
            return result;

        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
            pool.shutdown();
            return null;
        }
    }
}
