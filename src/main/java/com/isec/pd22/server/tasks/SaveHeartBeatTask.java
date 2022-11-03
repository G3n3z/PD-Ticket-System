package com.isec.pd22.server.tasks;

import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.InternalInfo;

public class SaveHeartBeatTask extends Thread{

    InternalInfo internalInfo;

    HeartBeat heartBeat;

    public SaveHeartBeatTask(InternalInfo internalInfo, HeartBeat heartBeat) {
        this.internalInfo = internalInfo;
        this.heartBeat = heartBeat;
    }

    @Override
    public void run() {
        //System.out.println("[SaveHeartBeatTask] - Começou o Save Heartbeat" );
        internalInfo.addHeartBeat(heartBeat);
        //System.out.println("[SaveHeartBeatTask] - Terminou o Save Heartbeat" );
    }
}
