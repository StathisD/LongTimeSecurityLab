package de.tu_darmstadt;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static de.tu_darmstadt.Parameters.*;


public class Main {


    public static void main(String[] args) {
        try {
            FILE_PATH = args[0];

            /*Scanner scanner = new Scanner(System.in);
            int input;
            do {
                show("Please specify what you want to do:");
                show("(Type 1 for creating a new connection to a Dealer)");
                input = scanner.nextInt();
            } while (input <= 0 || input > 1);

            switch (input) {
                case 1:
                    SSLShareHolder.startListeningForConnections();
            }*/
            SSLShareHolder.startListeningForConnections();


        /*Timestamp start = new Timestamp(System.currentTimeMillis());



        Timestamp end = new Timestamp(System.currentTimeMillis());
        System.out.println(end.getTime() - start.getTime());*/

        }catch(Exception e){
            e.printStackTrace();
        }

    }


}
