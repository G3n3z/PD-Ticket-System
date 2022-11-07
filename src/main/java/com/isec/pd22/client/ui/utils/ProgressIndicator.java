package com.isec.pd22.client.ui.utils;

import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ProgressIndicator extends javafx.scene.control.ProgressIndicator {

    double value = 0.0;

    double increment = 0.2;
    Runnable runnable = this::increment;
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    long delay  = 1000L;
    long period = 1000L;

    public ProgressIndicator() {
//        ProgressIndicator PI=new ProgressIndicator();
//        PI.setProgress(0.75f);
        executor.scheduleAtFixedRate(runnable, delay, period, TimeUnit.MILLISECONDS);
    }


    void increment(){
        if(value == 1.0){
            value = 0.2;

        }else{
            value += increment;
        }
        setProgress(value);
    }

    public void stop(){
        executor.shutdown();
    }


}
