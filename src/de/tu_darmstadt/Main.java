package de.tu_darmstadt;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    private static int NTHREADS = 8;
    private static int NBITS = 8;
    private static int NCHUNKS = 2;
    private static int CHUNKSIZE = 2; // in Bytes
    private static int SIZE = NCHUNKS * CHUNKSIZE;


    public static void main(String[] args) {

        Timestamp start = new Timestamp(System.currentTimeMillis());

        MappedByteBuffer mb = DataProvider.readData(args[0]);

        if (mb != null){
            ExecutorService pool = Executors.newCachedThreadPool();
            ArrayList<BigInteger> encodedData = new ArrayList<>();
            int nGet;
            while( mb.hasRemaining( ) )
            {
                Future<BigInteger[]>[] futures = new Future[NCHUNKS];
                nGet = Math.min( mb.remaining( ), SIZE );
                byte[] byteArray = new byte[nGet];
                mb.get( byteArray, 0, nGet );
                System.out.println(DatatypeConverter.printHexBinary(byteArray));

                // process byteArray
                for (int i = 0; i< NCHUNKS; i++){

                    if (nGet < SIZE){
                        byte[] chunk = Arrays.copyOfRange(byteArray, i* CHUNKSIZE, nGet);
                        System.out.println(DatatypeConverter.printHexBinary(chunk));

                        EncodingTask task = new EncodingTask(chunk, NBITS);
                        futures[i] = pool.submit(task);
                        break;
                    }else{
                        byte[] chunk = Arrays.copyOfRange(byteArray, i* CHUNKSIZE, (i+1)* CHUNKSIZE);
                        System.out.println(DatatypeConverter.printHexBinary(chunk));

                        EncodingTask task = new EncodingTask(chunk, NBITS);
                        futures[i] = pool.submit(task);
                    }

                }
                System.out.println("Join");

                try {

                    for (int i = 0; i< NCHUNKS; i++){
                        if (futures[i] != null){
                            BigInteger[] returnValue = futures[i].get();
                            System.out.println(Arrays.toString(returnValue));
                            Collections.addAll(encodedData, returnValue);

                        }
                    }

                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }



            }
            System.out.println(encodedData);
            BigInteger[] array = encodedData.toArray(new BigInteger[encodedData.size()]);
            DecodingTask decodingTask = new DecodingTask(array, NBITS);
            Future<byte[]> future = pool.submit(decodingTask);
            try {
                byte[] result = future.get();
                System.out.println(DatatypeConverter.printHexBinary(result));

            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }

            pool.shutdown();

            Timestamp end = new Timestamp(System.currentTimeMillis());
            System.out.println(end.getTime() - start.getTime());
        }





    }
}
