package com.isec.pd22.payload;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Role;
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
    Espetaculo espetaculo;
    private Set<HeartBeat> serverList;

    
    private ClientsPayloadType clientsPayloadType;

    public ClientMSG() {
        this.serverList = new HashSet<>();
    }

    public ClientMSG(ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
    }

    public User getUser() {
        return user;
    }
    public ClientActions getAction() {
        return action;
    }

    public void setAction(ClientActions action) {
        this.action = action;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public void setServerList(Set<HeartBeat> serverList) {
        this.serverList = serverList;
    }

    public Espetaculo getEspetaculo() {
        return espetaculo;
    }

    public void setEspetaculo(Espetaculo espetaculo) {
        this.espetaculo = espetaculo;
    }
    public ClientsPayloadType getClientsPayloadType() {
        return clientsPayloadType;
    }

}
