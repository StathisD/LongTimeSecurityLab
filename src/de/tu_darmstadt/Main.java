package de.tu_darmstadt;

import de.tu_darmstadt.Encryption.EncryptionTask;

import javax.xml.bind.DatatypeConverter;
import java.nio.MappedByteBuffer;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.tu_darmstadt.Constants.*;


public class Main {


    public static void main(String[] args) {

        Timestamp start = new Timestamp(System.currentTimeMillis());

        encrypt(args[0]);

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }

    public static void encrypt(String path) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try {
            MappedByteBuffer reader = DataReader.readData(path);
            MappedByteBuffer writer = DataWriter.writeData("/media/stathis/9AEA2384EA235BAF/testFile1", reader.limit());

            if (reader != null) {
                int nGet;
                while (reader.hasRemaining()) {
                    Future<byte[]>[] futures = new Future[NCHUNKS];
                    nGet = Math.min(reader.remaining(), SIZE);
                    byte[] byteArray = new byte[nGet];
                    reader.get(byteArray, 0, nGet);


                    // process byteArray
                    for (int i = 0; i < NCHUNKS; i++) {
                        if (nGet < (i + 1) * CHUNKSIZE) {
                            byte[] chunk = Arrays.copyOfRange(byteArray, i * CHUNKSIZE, nGet);
                            EncryptionTask task = new EncryptionTask(chunk, NBITS);
                            futures[i] = pool.submit(task);
                            break;
                        } else {
                            byte[] chunk = Arrays.copyOfRange(byteArray, i * CHUNKSIZE, (i + 1) * CHUNKSIZE);
                            EncryptionTask task = new EncryptionTask(chunk, NBITS);
                            futures[i] = pool.submit(task);
                        }

                    }

                    byte[] encryptedData = new byte[nGet];

                    for (int i = 0; i < NCHUNKS; i++) {
                        if (futures[i] != null) {
                            byte[] returnValue = futures[i].get();
                            System.arraycopy(returnValue, 0, encryptedData, i * CHUNKSIZE, returnValue.length);
                        }
                    }
                    if (!Arrays.equals(byteArray, encryptedData)) {
                        System.out.println("Data: " + DatatypeConverter.printHexBinary(byteArray));
                        System.out.println("Enc: " + DatatypeConverter.printHexBinary(encryptedData));
                        break;
                    }
                    //System.out.println("Enc: " + DatatypeConverter.printHexBinary(encryptedData));
                    writer.put(encryptedData);

                }


            }
        } catch (InterruptedException | ExecutionException ex) {
            ex.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}
