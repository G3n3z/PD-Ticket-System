package com.isec.pd22.server.models;

import com.isec.pd22.enums.Status;
import com.isec.pd22.utils.Constants;

import java.util.HashSet;
import java.util.Set;

public class InternalInfo {
    Status status;
    int portUdp;
    int numClients;
    int portTcp;
    Boolean finish;

    String url_db;

    String url;

    Set<ServerHeartBeat> heatBeats = new HashSet<>();


    public InternalInfo() {
    }

    public InternalInfo(String url_db, int portUdp, boolean finish) {
        this.portUdp = portUdp;
        this.finish = finish;
        this.url_db = Constants.BASE_URL_DB + url_db;
        this.url = Constants.BASE_URL + url_db;
    }


    public Set<ServerHeartBeat> getHeatBeats() {
        return heatBeats;
    }

    public void setHeatBeats(Set<ServerHeartBeat> heatBeats) {
        this.heatBeats = heatBeats;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl_db() {
        return url_db;
    }

    public void setUrl_db(String url_db) {
        this.url_db = url_db;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getPortUdp() {
        return portUdp;
    }

    public void setPortUdp(int portUdp) {
        this.portUdp = portUdp;
    }

    public int getNumClients() {
        return numClients;
    }

    public void setNumClients(int numClients) {
        this.numClients = numClients;
    }

    public int getPortTcp() {
        return portTcp;
    }

    public void setPortTcp(int portTcp) {
        this.portTcp = portTcp;
    }

    public Boolean isFinish() {
        return finish;
    }

    public void setFinish(boolean finish) {
        this.finish = finish;
    }

    /**
     * Incrementa o número de clientes que estão a ser atendidos pelo servidor
     */
    public void incrementNumClients() {
        numClients++;
    }

    /**
     * Decrementa o número de clientes que estão a ser atendidos pelo servidor
     */
    public void decrementNumClients() {
        numClients++;
    }

}
