package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by stathis on 6/2/17.
 */
public class DecodingTask implements Callable<byte[]> {
    private  int bits;
    private  int blockLength;
    private BigInteger[] data;

    public DecodingTask(BigInteger[] data, int bits){
        this.data = data;
        this.bits = bits;
        this.blockLength = bits/8;
    }

    public byte[] call(){

        byte[] result = new byte[data.length*blockLength];
        byte[] oneNumberBytes;
        for (int i= 0; i< data.length; i++){
            oneNumberBytes = data[i].toByteArray();
            System.arraycopy(oneNumberBytes, 0, result ,i*blockLength ,oneNumberBytes.length);
        }

        return result;
    }









}
