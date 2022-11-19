package com.isec.pd22.payload;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;

import java.util.Date;

public class HeartBeat extends MulticastMSG implements Comparable<HeartBeat>{
    String ip;
    int portTcpClients;
    int numOfClients;
    Status statusServer;
    int numVersionDB;
    int portUdp;

    long unixTimeSinceLastHeartBeat;

    public HeartBeat(String ip, int portTcpClients,
                     int numOfClients, Status statusServer, int numVersionDB, int portUdp) {
        super(TypeOfMulticastMsg.HEARTBEAT);
        this.ip = ip;
        this.portTcpClients = portTcpClients;
        this.numOfClients = numOfClients;
        this.statusServer = statusServer;
        this.numVersionDB = numVersionDB;
        this.portUdp = portUdp;
    }

    public HeartBeat(String ip, int portUdp) {
        super();
        this.ip = ip;
        this.portUdp = portUdp;
    }

    public long getUnixTimeSinceLastHeartBeat() {
        return unixTimeSinceLastHeartBeat;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortTcpClients() {
        return portTcpClients;
    }

    public void setPortTcpClients(int portTcpClients) {
        this.portTcpClients = portTcpClients;
    }

    public int getNumOfClients() {
        return numOfClients;
    }

    public void setNumOfClients(int numOfClients) {
        this.numOfClients = numOfClients;
    }

    public Status getStatusServer() {
        return statusServer;
    }

    public void setStatusServer(Status statusServer) {
        this.statusServer = statusServer;
    }

    public int getNumVersionDB() {
        return numVersionDB;
    }

    public void setNumVersionDB(int numVersionDB) {
        this.numVersionDB = numVersionDB;
    }

    public int getPortUdp() {
        return portUdp;
    }

    public void setPortUdp(int portUdp) {
        this.portUdp = portUdp;
    }

    @Override
    public int compareTo(HeartBeat o) {
        if(numVersionDB - o.numVersionDB != 0 ){
            return o.numVersionDB - numVersionDB;
        }
        return this.numOfClients - o.numOfClients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeartBeat heartBeat = (HeartBeat) o;

        if (getPortUdp() != heartBeat.getPortUdp()) return false;
        return getIp().equals(heartBeat.getIp());
    }

    @Override
    public int hashCode() {
        int result = getIp().hashCode();
        result = 31 * result + getPortUdp();
        return result;
    }

    @Override
    public String toString() {
        return "HeartBeat{" +
                "ip='" + ip + '\'' +
                ", portTcpClients=" + portTcpClients +
                ", numOfClients=" + numOfClients +
                ", statusServer=" + statusServer +
                ", numVersionDB=" + numVersionDB +
                ", portUdp=" + portUdp +
                ", typeMsg=" + typeMsg +
                ", unixTime="+ unixTimeSinceLastHeartBeat +
                ", date=" + new Date(unixTimeSinceLastHeartBeat) +
                '}';
    }

    public void setTimeMsg() {
        unixTimeSinceLastHeartBeat = new Date().getTime();
    }


    public void copyValues(HeartBeat o){
        ip = o.ip;
        numOfClients = o.numOfClients;
        portTcpClients = o.portTcpClients;
        statusServer = o.statusServer;
        numVersionDB = o.numVersionDB;
        portUdp = o.portUdp;
        typeMsg = o.typeMsg;
        unixTimeSinceLastHeartBeat = o.unixTimeSinceLastHeartBeat;

    }


}
