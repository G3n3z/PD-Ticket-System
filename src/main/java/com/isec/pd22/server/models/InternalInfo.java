package com.isec.pd22.server.models;

import com.isec.pd22.enums.Status;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.utils.Constants;

import java.net.Socket;
import java.util.ArrayList;
import java.net.MulticastSocket;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class InternalInfo {

    String ip;
    Status status;
    int portUdp;
    int numClients;
    int portTcp;
    Boolean finish;

    int numDB;
    String url_db;

    String url;

    MulticastSocket multicastSocket;

    // HeartBeat -> Server
    Set<HeartBeat> heartBeats = new HashSet<>();

    private ArrayList<Socket> allClientSockets;

    public Lock lock;

    public Condition condition;

    public InternalInfo() {
    }

    public InternalInfo(String url_db, int portUdp, boolean finish) {
        this.portUdp = portUdp;
        this.finish = finish;
        this.url_db = Constants.BASE_URL_DB + url_db;
        this.url = Constants.BASE_URL + url_db;
        this.allClientSockets = new ArrayList<>();
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public MulticastSocket getMulticastSocket() {
        return multicastSocket;
    }

    public void setMulticastSocket(MulticastSocket multicastSocket) {
        this.multicastSocket = multicastSocket;
    }

    public int getNumDB() {
        return numDB;
    }

    public void setNumDB(int numDB) {
        this.numDB = numDB;
    }

    public Set<HeartBeat> getHeatBeats() {
        return heartBeats;
    }

    public void setHeartBeats(Set<HeartBeat> heartBeats) {
        this.heartBeats = heartBeats;
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

    public ArrayList<Socket> getAllClientSockets() {
        return allClientSockets;
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

    public void addHeartBeat(HeartBeat heartBeat) {
        heartBeat.setTimeMsg();
        synchronized (heartBeats) {

            if (!heartBeats.contains(heartBeat)) {
                heartBeats.add(heartBeat);
                return;
            }

            for (HeartBeat heart : heartBeats) {
                if (heartBeat.equals(heart)) {
                    heart.copyValues(heartBeat);
                    break;
                }
            }
        }
    }

    public void checkServersLastBeatMore35Sec() {
        long now = new Date().getTime();
        synchronized (this.heartBeats){
            heartBeats.removeIf( heartBeat -> (now - heartBeat.getUnixTimeSinceLastHeartBeat()) >= 35000);
        }
    }
}
