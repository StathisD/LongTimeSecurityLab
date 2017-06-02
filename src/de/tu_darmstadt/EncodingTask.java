package de.tu_darmstadt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Created by stathis on 5/31/17.
 */
public class EncodingTask implements Callable<BigInteger[]>{
    private  int bits;
    private  int blockLength;
    private byte[] data;

    public EncodingTask(byte[] data, int bits){
        this.data = data;
        this.bits = bits;
        this.blockLength = bits/8;
    }

    public BigInteger[] call(){

        int encodedSize = data.length/blockLength;
        BigInteger[] encodedData = new BigInteger[encodedSize];
        byte[] oneNumber;

        for (int i = 0; i< encodedSize; i ++){
            if ((i+1)*blockLength <= data.length){
                oneNumber = Arrays.copyOfRange(data,i*blockLength,(i+1)*blockLength);
            }else{
                oneNumber = Arrays.copyOfRange(data,i*blockLength,data.length);
            }
            encodedData[i] = new BigInteger(oneNumber);

        }

        return encodedData;
    }

    /*private static byte[] addPadding(byte[] data){

        int mod = data.length % blockLength;

        if (mod != 0){
            byte[] oldData = data;
            data = new byte[oldData.length + (blockLength - mod) ];
            System.arraycopy(oldData,0,data,0, oldData.length - mod);
            System.arraycopy(oldData,oldData.length - mod, data, (oldData.length -mod)+ (blockLength - mod), mod);
        }

        return data;

    }*/








}
