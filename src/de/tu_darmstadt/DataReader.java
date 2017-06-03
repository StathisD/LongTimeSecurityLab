package de.tu_darmstadt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by stathis on 5/31/17.
 */
public class DataReader {

    public static MappedByteBuffer readData(String path){

        File file = new File(path);

        MappedByteBuffer mb;

        try {

            FileChannel ch =  new RandomAccessFile(file, "r").getChannel();
            mb = ch.map( FileChannel.MapMode.READ_ONLY, 0L, ch.size( ) );

        }
        catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
            return null;
        }

        catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
            return null;
        }

        return mb;
    }

}
