package de.tu_darmstadt;


import com.j256.ormlite.dao.CloseableIterator;

import java.util.Random;

import static de.tu_darmstadt.Parameters.*;


/**
 * Created by Stathis on 7/1/17.
 */
public class RenewShareThread extends Thread {

    public void run(){
        try{
            while(true){
                // check if renewal in Process
                if (!RenewShareTask.active){
                    // find all stored shares
                    dbSemaphore.acquire();
                    CloseableIterator<Share> iterator = sharesDao.closeableIterator();
                    dbSemaphore.release();
                    try {
                        // iterate over shares and check if they must be renewed
                        while (iterator.hasNext()) {
                            dbSemaphore.acquire();
                            Share share = iterator.next();
                            dbSemaphore.release();
                            // default RENEWAL_INTERVAL = 1 day
                            if (Math.abs(share.getLastRenewed() - System.currentTimeMillis()) >= (RENEWAL_INTERVAL) && !share.getRenewStatus().equals("in progress")) {
                                share.setRenewStatus("needs renewal");
                                dbSemaphore.acquire();
                                sharesDao.update(share);
                                dbSemaphore.release();

                                //compute backoff, max 1 min
                                long backoffTime = (long) (new Random().nextFloat() * (1000 * 60 * 5));
                                show(backoffTime);
                                Thread.sleep(backoffTime);
                                // check if share still exists after sleep
                                dbSemaphore.acquire();
                                share = sharesDao.queryForId(share.getName());
                                dbSemaphore.release();
                                if ( share != null){
                                    if (!RenewShareTask.active && share.getRenewStatus().equals("needs renewal")) {
                                        // start renewal
                                        new RenewShareTask(share).start();
                                    }
                                }
                            }
                        }
                    } finally {
                        // close it at the end to close underlying SQL statement
                        iterator.close();
                    }
                }
                sleep(timeSlot);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
