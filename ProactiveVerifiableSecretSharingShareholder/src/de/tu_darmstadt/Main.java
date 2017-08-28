package de.tu_darmstadt;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.*;

import static de.tu_darmstadt.Database.lookupShareHoldersForShare;
import static de.tu_darmstadt.Database.lookupXvalueForShareHolderAndShare;
import static de.tu_darmstadt.Parameters.*;



public class Main {


    public static void main(String[] args) {
        try {
            int serverNr = Integer.valueOf(args[1]);
            if (serverNr > 0 && serverNr < 10){
                SERVER_NR = serverNr;
                ports = new int[]{8001+(serverNr-1)*10, 8002+(serverNr-1)*10, 8003+(serverNr-1)*10, 8004+(serverNr-1)*10, 8005+(serverNr-1)*10, 8006+(serverNr-1)*10, 8007+(serverNr-1)*10, 8008+(serverNr-1)*10, 8009+(serverNr-1)*10, 8010+(serverNr-1)*10};
                SERVER_NAME = "Server" + args[1];
                SHARE_DIR = args[0] + SERVER_NAME+"/";
            }else{
                show("Server Number must be between 1 and 10");
                return;
            }

            Database.initiateDb();

            dbSemaphore.acquire();
            PedersenParameters params = pedersenParametersDao.queryForId("params");
            dbSemaphore.release();
            committer = new PedersenCommitter(params);
            MODULUS = params.getQ();
            pedersenParameters = params;

            new RenewShareThread().start();

            ServerListener.startListeningForConnections();

            //fun();


        }catch(Exception e){
            e.printStackTrace();
        }

    }
public static void fun(){
        try{
            RandomAccessFile sourceFile1 = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/Server1/file", "r");
            RandomAccessFile destinationFile1 = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/Server1/file"+"_new", "rw");
            sourceFile1.seek(0L);
            destinationFile1.seek(0L);
            long targetFileSize = sourceFile1.length();
            RandomAccessFile sourceFile2 = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/Server2/file", "r");
            RandomAccessFile destinationFile2 = new RandomAccessFile("/media/stathis/9AEA2384EA235BAF/Server2/file"+"_new", "rw");
            sourceFile2.seek(0L);
            destinationFile2.seek(0L);

            List<ShareHolder> shareHolderList1 = new ArrayList<>();
            ShareHolder localShareholder1 = new ShareHolder("Server1","localhost",0);
            localShareholder1.setxValue(BigInteger.valueOf(1));
            shareHolderList1.add(localShareholder1);
            localShareholder1 = new ShareHolder("Server2","localhost",0);
            localShareholder1.setxValue(BigInteger.valueOf(2));
            shareHolderList1.add(localShareholder1);

            List<ShareHolder> shareHolderList2 = new ArrayList<>();
            ShareHolder localShareholder2 = new ShareHolder("Server2","localhost",0);
            localShareholder2.setxValue(BigInteger.valueOf(2));
            shareHolderList2.add(localShareholder2);
            localShareholder2 = new ShareHolder("Server1","localhost",0);
            localShareholder2.setxValue(BigInteger.valueOf(1));
            shareHolderList2.add(localShareholder2);

            initializeParameters(targetFileSize, 0, true);

            long numbersInShare = targetFileSize / SHARE_SIZE;

            long numbersRenewed = 0;

            while(numbersRenewed < numbersInShare) {
                int numbersInBuffer;
                if (numbersInShare - numbersRenewed < 8000) {
                    numbersInBuffer = (int) (numbersInShare - numbersRenewed);
                } else {
                    numbersInBuffer = 8000;
                }

                HashMap<String, BigInteger[]> remoteNumberMap1 = new HashMap<>();

                HashMap<String, BigInteger[]> localNumberMap1 = new HashMap<>();


                HashMap<String, BigInteger[]> remoteNumberMap2 = new HashMap<>();

                HashMap<String, BigInteger[]> localNumberMap2 = new HashMap<>();
                int step;
                BigInteger[] remoteNumbers;
                if (VERIFIABILITY) {
                    step = 2 + 2;
                    remoteNumbers = new BigInteger[numbersInBuffer * (2 + 2)];
                } else {
                    step = 1;
                    remoteNumbers = new BigInteger[numbersInBuffer];
                }

                for (int k = 0; k < numbersInBuffer; k++) {

                    BigIntegerPolynomial currentPolynomial = new BigIntegerPolynomial(2 - 1, MODULUS, BigInteger.ZERO);

                    List<BigInteger> list = new ArrayList<BigInteger>();

                    BigInteger yValue = currentPolynomial.evaluatePolynom(BigInteger.valueOf(2));
                    list.add(yValue);
                    if (VERIFIABILITY) {
                        BigInteger shareCommitment = currentPolynomial.G.evaluatePolynom(BigInteger.valueOf(2));
                        list.add(shareCommitment);
                        list.addAll(Arrays.asList(currentPolynomial.commitments));
                    }
                    BigInteger[] share = list.toArray(new BigInteger[list.size()]);
                    System.arraycopy(share, 0, remoteNumbers, k*share.length, share.length);

                    /*

                    remoteNumbers[k] = currentPolynomial.evaluatePolynom(BigInteger.valueOf(2));
                    remoteNumbers[k + 1] = currentPolynomial.G.evaluatePolynom(BigInteger.valueOf(2));
                    System.arraycopy(currentPolynomial.commitments, 0, remoteNumbers, k + 2, currentPolynomial.commitments.length);
                    */

                }
                remoteNumberMap1.put("Server2", remoteNumbers);

                new ProactiveVerificationTask(2, 2, remoteNumberMap1.get("Server2")).call();

                numbersRenewed += numbersInBuffer;
            }
            show("Renewal completed");
        }catch (Exception e){
            e.printStackTrace();
        }

}



}
