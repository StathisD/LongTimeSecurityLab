package de.tu_darmstadt.Encoder;

import de.tu_darmstadt.DataProvider;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Constants.*;

/**
 * Created by stathis on 6/3/17.
 */
public class Encoder {

    public static ArrayList<BigInteger> encode(String path) {
        MappedByteBuffer mb = DataProvider.readData(path);
        ArrayList<BigInteger> encodedData = new ArrayList<>();
        if (mb != null) {
            ExecutorService pool = Executors.newCachedThreadPool();
            int nGet;
            while (mb.hasRemaining()) {
                Future<BigInteger[]>[] futures = new Future[NCHUNKS];
                nGet = Math.min(mb.remaining(), SIZE);
                byte[] byteArray = new byte[nGet];
                mb.get(byteArray, 0, nGet);
                System.out.println(DatatypeConverter.printHexBinary(byteArray));

                // process byteArray
                for (int i = 0; i < NCHUNKS; i++) {

                    if (nGet < SIZE) {
                        byte[] chunk = Arrays.copyOfRange(byteArray, i * CHUNKSIZE, nGet);
                        System.out.println(DatatypeConverter.printHexBinary(chunk));

                        EncodingTask task = new EncodingTask(chunk, NBITS);
                        futures[i] = pool.submit(task);
                        break;
                    } else {
                        byte[] chunk = Arrays.copyOfRange(byteArray, i * CHUNKSIZE, (i + 1) * CHUNKSIZE);
                        System.out.println(DatatypeConverter.printHexBinary(chunk));

                        EncodingTask task = new EncodingTask(chunk, NBITS);
                        futures[i] = pool.submit(task);
                    }

                }
                System.out.println("Join");

                try {

                    for (int i = 0; i < NCHUNKS; i++) {
                        if (futures[i] != null) {
                            BigInteger[] returnValue = futures[i].get();
                            System.out.println(Arrays.toString(returnValue));
                            Collections.addAll(encodedData, returnValue);

                        }
                    }

                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }


            }
            pool.shutdown();
        }
        return encodedData;
    }
}

