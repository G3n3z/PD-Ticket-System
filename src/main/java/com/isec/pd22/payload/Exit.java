package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class Exit extends MulticastMSG{

    HeartBeat heartBeat;

    public Exit(HeartBeat heartBeat) {
        super(TypeOfMulticastMsg.EXIT);
        this.heartBeat = heartBeat;
    }

    public void setHeartBeat(HeartBeat heartBeat) {
        this.heartBeat = heartBeat;
    }

    public HeartBeat getHeartBeat() {
        return heartBeat;
    }
}
