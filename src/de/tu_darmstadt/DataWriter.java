package de.tu_darmstadt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by stathis on 6/3/17.
 */
public class DataWriter {


    public static MappedByteBuffer writeData(String path, int fileSize) {

        File file = new File(path);

        file.delete();

        MappedByteBuffer mb;

        try {

            FileChannel ch = new RandomAccessFile(file, "rw").getChannel();
            mb = ch.map(FileChannel.MapMode.READ_WRITE, 0L, fileSize);

        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
            return null;
        } catch (IOException ioe) {
            System.out.println("Exception while reading file " + ioe);
            return null;
        }

        return mb;
    }

}