package com.isec.pd22.rmi;

import com.isec.pd22.payload.HeartBeat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

public class RmiServidor implements Serializable {


    @Serial
    private static final long serialVersionUID= 1L;
    String ip;
    int portTcp;
    int portUdp;
    long timeAtLastHeartbeat;

    public RmiServidor() {
    }

    public RmiServidor(String ip, int portTcp, int portUdp, long timeAtLastHeartbeat) {
        this.ip = ip;
        this.portTcp = portTcp;
        this.portUdp = portUdp;
        this.timeAtLastHeartbeat = timeAtLastHeartbeat;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortTcp() {
        return portTcp;
    }

    public void setPortTcp(int portTcp) {
        this.portTcp = portTcp;
    }

    public int getPortUdp() {
        return portUdp;
    }

    public void setPortUdp(int portUdp) {
        this.portUdp = portUdp;
    }

    public long getTimeAtLastHeartbeat() {
        return timeAtLastHeartbeat;
    }

    public void setTimeAtLastHeartbeat(long timeAtLastHeartbeat) {
        this.timeAtLastHeartbeat = timeAtLastHeartbeat;
    }

    @Override
    public String toString() {
        return "Servidor{" +
                "ip='" + ip + '\'' +
                ", portTcp=" + portTcp +
                ", portUdp=" + portUdp +
                ", timeAtLastHeartbeat=" + timeAtLastHeartbeat +
                "}\n";
    }

    public static RmiServidor mapToRmiObject(HeartBeat heartBeat, String ip, int port) {
        RmiServidor server = new RmiServidor();
        server.ip = heartBeat.getIp();
        server.portTcp = heartBeat.getPortTcpClients();
        server.portUdp = heartBeat.getPortUdp();
        if(server.ip.equals(ip) && server.portUdp == port){
            server.timeAtLastHeartbeat = 0;
        }else{
            server.timeAtLastHeartbeat = (new Date().getTime() - heartBeat.getUnixTimeSinceLastHeartBeat())/1000;
        }
        return server;
    }
}
