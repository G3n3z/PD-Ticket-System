package com.isec.pd22.payload.tcp;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.User;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.server.models.ServerHeartBeat;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ClientMSG implements Serializable {
    ClientActions action;
    User user;
    private Set<HeartBeat> serverList;
    private ClientsPayloadType clientsPayloadType;

    private String message;

    public ClientMSG(ClientActions action, ClientsPayloadType clientsPayloadType) {
        this.action = action;
        this.clientsPayloadType = clientsPayloadType;
    }

    public ClientMSG(ClientActions action, ClientsPayloadType clientsPayloadType, String message) {
        this.action = action;
        this.clientsPayloadType = clientsPayloadType;
        this.message = message;
    }

    public ClientMSG(ClientActions action) {
        this.action = action;
    }


    public ClientMSG() {
        this.serverList = new HashSet<>();
    }

    public ClientMSG(ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
    }


    public void setClientsPayloadType(ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
    }

    public void setAction(ClientActions action) {
        this.action = action;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ClientActions getAction() {
        return action;
    }


    public void setServerList(Set<HeartBeat> serverList) {
        this.serverList = serverList;
    }

    public ClientsPayloadType getClientsPayloadType() {
        return clientsPayloadType;
    }

    public String getMessage() {
        return message;
    }

}
