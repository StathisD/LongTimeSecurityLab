package de.tu_darmstadt;

import static de.tu_darmstadt.Parameters.*;



public class Main {


    public static void main(String[] args) {
        try {
            // Set server Number
            int serverNr = Integer.valueOf(args[1]);
            if (serverNr > 0 && serverNr < 10){
                SERVER_NR = serverNr;
                // set server ports
                ports = new int[]{8001+(serverNr-1)*10, 8002+(serverNr-1)*10, 8003+(serverNr-1)*10, 8004+(serverNr-1)*10, 8005+(serverNr-1)*10, 8006+(serverNr-1)*10, 8007+(serverNr-1)*10, 8008+(serverNr-1)*10, 8009+(serverNr-1)*10, 8010+(serverNr-1)*10};
                SERVER_NAME = "Server" + args[1];
                SHARE_DIR = args[0] + SERVER_NAME+"/";
                LOCAL_DIR = args[0];
            }else{
                show("Server Number must be between 1 and 10");
                return;
            }

            // initialize DB objects
            Database.initiateDb();

            // initialize Pedersen Parameters
            dbSemaphore.acquire();
            PedersenParameters params = pedersenParametersDao.queryForId("params");
            dbSemaphore.release();
            committer = new PedersenCommitter(params);
            MODULUS = params.getQ();
            pedersenParameters = params;

            // start Renewal monitoring process
            new RenewShareThread().start();

            // start TLS Server Sockets
            ServerListener.startListeningForConnections();


        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
