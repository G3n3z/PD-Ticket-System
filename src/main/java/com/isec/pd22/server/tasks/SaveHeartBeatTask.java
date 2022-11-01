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
        internalInfo.addHeartBeat(heartBeat);
    }
}
