package de.tu_darmstadt;

import static de.tu_darmstadt.Parameters.*;



public class Main {


    public static void main(String[] args) {
        try {
            FILE_PATH = args[0];

            Database.initiateDb();

            dbSemaphore.acquire();
            PedersenParameters params = pedersenParametersDao.queryForId("params");
            dbSemaphore.release();
            committer = new PedersenCommitter(params);
            MODULUS = params.getQ();
            pedersenParameters = params;

            new RenewShareThread().start();

            ServerListener.startListeningForConnections();


        }catch(Exception e){
            e.printStackTrace();
        }

    }




}
