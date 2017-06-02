package de.tu_darmstadt;

import de.tu_darmstadt.Decoder.Decoder;
import de.tu_darmstadt.Encoder.Encoder;

import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;


public class Main {


    public static void main(String[] args) {

        Timestamp start = new Timestamp(System.currentTimeMillis());

        ArrayList<BigInteger> encodedData = Encoder.encode(args[0]);

        System.out.println(encodedData);

        byte[] result = Decoder.decode(encodedData);

        System.out.println(DatatypeConverter.printHexBinary(result));

        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());

    }
}
