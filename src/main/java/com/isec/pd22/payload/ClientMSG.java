package com.isec.pd22.payload;

import com.isec.pd22.server.models.ServerHeartBeat;

import java.util.HashSet;
import java.util.Set;

public class ClientMSG {
    private String command; //string de onde virá a query do cliente e onde seguirá a resposta do servidor
    private Set<ServerHeartBeat> serverList;

    public ClientMSG() {}

    public ClientMSG(String command) {
        this.command = command;
        this.serverList = new HashSet<>();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Set<ServerHeartBeat> getServerList() {
        return serverList;
    }

    public void setServerList(Set<ServerHeartBeat> serverList) {
        this.serverList = serverList;
    }
}
