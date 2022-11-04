package com.isec.pd22.client.models;

import com.isec.pd22.client.threads.ServerConnectionThread;
import com.isec.pd22.payload.HeartBeat;

import java.net.Socket;
import java.util.List;

public class ClientModel {

    private int udpServerPort;
    private String udpServerIp;
    private List<HeartBeat> serversList;
    private ConnectionModel tcpServerConnection;
    private ServerConnectionThread serverConnectionThread;

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

    public String getUdpServerIp() {
        return udpServerIp;
    }

    public List<HeartBeat> getServersList() { return serversList; }

    public void setServersList(List<HeartBeat> serversList) { this.serversList = serversList; }

    public ConnectionModel getTcpServerConnection() {
        return tcpServerConnection;
    }

    public void setTcpServerConnection(ConnectionModel tcpServerConnection) {
        synchronized (this) {
            this.tcpServerConnection = tcpServerConnection;
        }
    }

    public ServerConnectionThread getServerConnectionThread() {
        return serverConnectionThread;
    }

    public void setServerConnectionThread(ServerConnectionThread serverConnectionThread) {
        this.serverConnectionThread = serverConnectionThread;
    }
}
