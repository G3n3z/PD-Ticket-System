package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.server.models.Query;

import java.net.Socket;
import java.util.Objects;

public class Prepare extends MulticastMSG{
    Query query;
    int numVersion;
    int confirmationUDPPort;
    String ip;
    int portUdpClients;

    public Prepare() {
    }

    public Prepare(TypeOfMulticastMsg typeMsg, Query query, int numVersion, String ip, int confirmationUDPPort) {
        super(typeMsg);
        this.query = query;
        this.numVersion = numVersion;
        this.ip = ip;
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


    public void setConfirmationUDPPort(int confirmationUDPPort) {
        this.confirmationUDPPort = confirmationUDPPort;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortUdpClients() {
        return portUdpClients;
    }

    public void setPortUdpClients(int portUdpClients) {
        this.portUdpClients = portUdpClients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prepare prepare = (Prepare) o;
        return portUdpClients == prepare.portUdpClients && ip.equals(prepare.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, portUdpClients);
    }
}
