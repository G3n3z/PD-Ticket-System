package com.isec.pd22.payload;

import com.isec.pd22.enums.Status;
import com.isec.pd22.server.models.ServerHeartBeat;

public class HeartBeat extends MulticastMSG implements Comparable<HeartBeat>{
    String ip;
    int portTcpClients;
    int numOfClients;
    Status statusServer;
    int numVersionDB;
    int portUdp;


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
}
