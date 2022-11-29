package com.isec.pd22.payload.tcp;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.User;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.server.models.ServerHeartBeat;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientMSG implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    ClientActions action;
    User user;
    private List<HeartBeat> serverList;
    private ClientsPayloadType clientsPayloadType;
    private ClientMSG lastSubscription;
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
        this.serverList = new ArrayList<>();
    }

    public ClientMSG(ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
    }


    public void setMessage(String message) {
        this.message = message;
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

    public List<HeartBeat> getServerList() {
        return serverList;
    }

    public void setServerList(List<HeartBeat> serverList) {
        this.serverList = serverList;
    }

    public ClientsPayloadType getClientsPayloadType() {
        return clientsPayloadType;
    }

    public String getMessage() {
        return message;
    }

    public ClientMSG getSubscription() {
        return lastSubscription;
    }

    public void setSubscription(ClientMSG lastMessageReceive) {
        lastSubscription = lastMessageReceive;
    }
}
