package com.isec.pd22.server.threads;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.ObjectStream;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatTask  extends TimerTask {

    InternalInfo internalInfo;
    MulticastSocket multicastSocket;
    ObjectStream os;

    Timer timer;

    boolean onlyOnceTime;


    public HeartBeatTask(InternalInfo internalInfo, Timer timer) {
        this.internalInfo = internalInfo;
        multicastSocket = internalInfo.getMulticastSocket();
        this.onlyOnceTime = true;
        os = new ObjectStream();
        this.timer = timer;
    }

    public HeartBeatTask(InternalInfo internalInfo) {
        this.internalInfo = internalInfo;
        multicastSocket = internalInfo.getMulticastSocket();
        this.onlyOnceTime = false;
        os = new ObjectStream();
    }


    @Override
    public void run() {
        HeartBeat heartBeat = new HeartBeat(TypeOfMulticastMsg.HEARTBEAT, internalInfo.getIp(), internalInfo.getPortTcp(),
                internalInfo.getNumClients(), internalInfo.getStatus(), internalInfo.getNumDB(), internalInfo.getPortUdp());
        byte bytes [] = new byte[1000];
        DatagramPacket packet = null;
        try {
            packet = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            os.writeObject(packet, heartBeat);
            multicastSocket.send(packet);
        } catch (UnknownHostException e) {
            System.out.println("Erro ao enviar sinal de vida");
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("[heartbeat task]Nao conseguiu enviar o datagrampacket " + new Date());
        }

        //Se for para executar mais que uma vez
        if (!onlyOnceTime){
            internalInfo.checkServersLastBeatMore35Sec();
        }
        else{
            cancel();
            timer.cancel();
        }
        System.out.println("Enviado HeartBeat "+ new Date());
    }
}
