package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.server.models.Query;

import java.net.Socket;

public class Prepare extends MulticastMSG{
    Query query;
    int numVersion;
    int confirmationUDPPort;
    String ip;
    int portTcpClients;

    public Prepare() {
    }

    public Prepare(TypeOfMulticastMsg typeMsg, Query query, int numVersion, int confirmationUDPPort) {
        super(typeMsg);
        this.query = query;
        this.numVersion = numVersion;
        this.confirmationUDPPort = confirmationUDPPort;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public int getNumVersion() {
        return numVersion;
    }

    public void setNumVersion(int numVersion) {
        this.numVersion = numVersion;
    }

    public int getConfirmationUDPPort() {
        return this.confirmationUDPPort;
    }

    public void setConfirmationUDPSocket(int confirmationUDPport) {
        this.confirmationUDPPort = confirmationUDPPort;
    }
}
