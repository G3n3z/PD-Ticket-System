package com.isec.pd22.server.models;

import com.isec.pd22.enums.Status;
import com.isec.pd22.interfaces.Observer;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.threads.AttendClientThread;
import com.isec.pd22.utils.Constants;

import java.net.Socket;
import java.sql.Connection;
import java.util.*;
import java.net.MulticastSocket;
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
    private ArrayList<AttendClientThread> clientThreads;

    public Lock lock;

    public Condition condition;

    Set<Observer> observers;

    Connection connection;

    public InternalInfo() {
    }

    public InternalInfo(String url_db, int portUdp, boolean finish) {
        this.portUdp = portUdp;
        this.finish = finish;
        this.url_db = Constants.BASE_URL_DB + url_db;
        this.url = Constants.BASE_URL + url_db;
        this.allClientSockets = new ArrayList<>();
        this.clientThreads = new ArrayList<>();
        this.observers = new HashSet<>();
    }

    public Set<Observer> getObservers() {
        return observers;
    }

    public void addClientThread(AttendClientThread thread){
        synchronized (clientThreads){
            clientThreads.add(thread);
        }
    }

    public void removeClientThread(AttendClientThread thread){
        synchronized (clientThreads){
            clientThreads.remove(thread);
        }
    }

    public void closeInputStreams(){
        ListIterator<AttendClientThread> it = clientThreads.listIterator(clientThreads.size());
        AttendClientThread temp;
        while (it.hasPrevious())
        {
            temp = it.previous();
            temp.closeThread();
            it.remove();
        }
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

    public ArrayList<HeartBeat> getOrderedHeatBeats() {
        ArrayList<HeartBeat> servers = new ArrayList<>(heartBeats);

        Collections.sort(servers);

        return servers;
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
        numClients--;
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

    public void addObserver(Observer observer) {
        synchronized (observers){
            observers.add(observer);
        }
    }

    public void removeObserver(Observer observer){
        synchronized (observers){
            observers.remove(observer);
        }
    }

    public void notifyAllObservers(){
        System.out.println("Vou notificar");
        synchronized (observers){
            Iterator<Observer> it =  observers.iterator();
            while (it.hasNext()){
                Observer observer = it.next();
                try {
                    observer.update();
                }catch (Exception e){
                    it.remove();
                }
            }
        }
    }

    public void removeHeatBeat(HeartBeat heartBeat) {
        synchronized (heartBeats){
            heartBeats.remove(heartBeat);
        }
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }


    public void setMyStatus(Status status) {
        synchronized (heartBeats){
            heartBeats.forEach( heartBeat -> {
                if(heartBeat.getIp().equals(ip) && heartBeat.getPortUdp() == portUdp){
                    heartBeat.setStatusServer(status);
                }
            } );
        }
    }
}
