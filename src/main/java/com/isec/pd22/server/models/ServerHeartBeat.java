package com.isec.pd22.server.models;

import com.isec.pd22.enums.Status;

public class ServerHeartBeat {

    String ip;
    int portTcpClients;
    int numOfClients;
    Status statusServer;
    int numVersionDB;
    int portTcp;

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

    public int getPortTcp() {
        return portTcp;
    }

    public void setPortTcpUpdateDB(int portTcp) {
        this.portTcp = portTcp;
    }
}
