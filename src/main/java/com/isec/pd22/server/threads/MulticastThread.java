package com.isec.pd22.server.threads;

import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.tasks.SaveHeartBeatTask;
import com.isec.pd22.server.tasks.UpdateDBTask;
import com.isec.pd22.utils.ObjectStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Timer;

public class MulticastThread extends Thread{


    InternalInfo internalInfo;
    Timer timer;

    ObjectStream objectStream;

    DatagramPacket packet;

    MulticastSocket multicastSocket;

    byte [] bytes;

    public MulticastThread(InternalInfo internalInfo, Timer timer) {
        this.internalInfo = internalInfo;
        this.multicastSocket = internalInfo.getMulticastSocket();
        this.timer = timer;
        objectStream = new ObjectStream();
        bytes = new byte[2000];
        packet = new DatagramPacket(bytes, bytes.length);
    }

    @Override
    public void run() {
        while (true){
            if(internalInfo.isFinish()){
                break;
            }
            try {
                multicastSocket.receive(packet);
                MulticastMSG msg = objectStream.readObject(packet, MulticastMSG.class);

                switch (msg.getTypeMsg()){
                    case HEARTBEAT -> {
                        HeartBeat heartBeat = (HeartBeat) msg;
                        new SaveHeartBeatTask(internalInfo, heartBeat).start();
                    }
                    case UPDATE_DB -> {
                        UpdateDB msgUpdate = (UpdateDB) msg;
                        new UpdateDBTask(internalInfo, msgUpdate).start();
                    }
                    case PREPARE -> {

                    }
                    case ABORT -> {

                    }
                    case COMMIT -> {

                    }
                }



            } catch (IOException e) {

            }

        }
        System.out.println("A sair da thread multicast");
    }
}
