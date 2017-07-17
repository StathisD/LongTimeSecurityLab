package de.tu_darmstadt;


import com.j256.ormlite.dao.CloseableIterator;

import static de.tu_darmstadt.Parameters.*;


/**
 * Created by Stathis on 7/1/17.
 */
public class RenewShareThread extends Thread {

    public void run(){
        try{
            while(true){
                dbSemaphore.acquire();
                CloseableIterator<Share> iterator = sharesDao.closeableIterator();
                try {
                    while (iterator.hasNext()) {
                        Share share = iterator.next();
                        if (Math.abs(share.getLastRenewed()-System.currentTimeMillis()) >= (timeSlot-1000*60*60*24) && !share.getRenewStatus().equals("in_progress")){
                            share.setRenewStatus("in_progress");
                            sharesDao.update(share);
                            new RenewShareTask(share).start();
                        }
                    }
                } finally {
                    // close it at the end to close underlying SQL statement
                    iterator.close();
                }
                dbSemaphore.release();
                sleep(timeSlot);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
