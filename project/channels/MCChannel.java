package project.channels;

import java.lang.Runnable;
import java.util.concurrent.TimeUnit;

public class MCChannel extends Channel implements Runnable{

    public MCChannel(String MCCAddr, short peerId, float version) throws Exception {
        super(MCCAddr, peerId, version);
    }

    @Override
    public void run() {
        try {
            while(true) {
                System.out.println("Executing: MCC");
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}