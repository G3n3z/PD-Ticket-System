package com.isec.pd22.client.models;

import com.isec.pd22.payload.HeartBeat;

import java.net.Socket;
import java.util.List;

public class ClientModel {

    private int udpServerPort;
    private String udpServerIp;

    private List<HeartBeat> serversList;

    private Socket tcpServerSocket;

    public ClientModel(String[] args) throws RuntimeException {
        if (args.length != 2) {
            throw new RuntimeException("" +
                    "Deve fornecer os seguintes dados do servidor de conexão:" +
                    " ip porto");
        }

        udpServerIp = args[0];
        udpServerPort = Integer.parseInt(args[1]);
    }

    public int getUdpServerPort() {
        return udpServerPort;
    }

    public void setUdpServerPort(int udpServerPort) {
        this.udpServerPort = udpServerPort;
    }

    public String getUdpServerIp() {
        return udpServerIp;
    }

    public void setUdpServerIp(String udpServerIp) {
        this.udpServerIp = udpServerIp;
    }

    public List<HeartBeat> getServersList() { return serversList; }

    public void setServersList(List<HeartBeat> serversList) { this.serversList = serversList; }
}
