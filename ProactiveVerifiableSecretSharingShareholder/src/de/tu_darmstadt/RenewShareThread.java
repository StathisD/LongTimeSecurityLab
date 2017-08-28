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
                if (!RenewShareTask.active){
                    dbSemaphore.acquire();
                    CloseableIterator<Share> iterator = sharesDao.closeableIterator();
                    dbSemaphore.release();
                    try {
                        while (iterator.hasNext()) {
                            dbSemaphore.acquire();
                            Share share = iterator.next();
                            dbSemaphore.release();
                            if (Math.abs(share.getLastRenewed() - System.currentTimeMillis()) >= (1000 * 60 * 60 * 24) && !share.getRenewStatus().equals("in progress")) {
                                share.setRenewStatus("needs renewal");
                                dbSemaphore.acquire();
                                sharesDao.update(share);
                                dbSemaphore.release();

                                //copmpute backoff, max 10 min
                                long backoffTime = (long) (new Random().nextFloat() * (1000 * 60));
                                show(backoffTime);
                                Thread.sleep(backoffTime);
                                dbSemaphore.acquire();
                                share = sharesDao.queryForId(share.getName());
                                dbSemaphore.release();
                                if ( share != null){
                                    if (!RenewShareTask.active && share.getRenewStatus().equals("needs renewal")) {
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
