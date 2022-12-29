package com.isec.pd22.rmi;

import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.InternalInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerRmiService extends UnicastRemoteObject implements ServerRemoteInterface {

    Map<String, ClientRemoteInterface> observers;
    String ip; int port;
    Set<HeartBeat> heartBeat;
    public ServerRmiService(InternalInfo internalInfo) throws RemoteException {
        this.heartBeat = internalInfo.getHeatBeats();
        observers = new HashMap<>();
        this.ip = internalInfo.getIp();
        this.port = internalInfo.getPortUdp();
    }

    @Override
    public boolean registerToNotifications(String uuid, ClientRemoteInterface clientRemote) throws RemoteException {
        observers.put(uuid, clientRemote);
        return true;
    }

    @Override
    public boolean unregisterToNotifications(String uuid, ClientRemoteInterface clientRemote) throws RemoteException {
        observers.remove(uuid);
        return true;
    }

    @Override
    public RmiServerMessage getListOfServers() throws RemoteException{
        RmiServerMessage serverMessage = new RmiServerMessage();
        List<RmiServidor> list;
        synchronized (heartBeat){
            list = heartBeat.stream().map( h -> RmiServidor.mapToRmiObject(h, ip, port)).toList();
        }
        serverMessage.setServers(list);
        return serverMessage;
    }

    public void notifyAllObservers(String message){
        List<String> idsOfClienteFailed = new ArrayList<>();
        for(Map.Entry<String, ClientRemoteInterface> entry : observers.entrySet()){
            try {
                entry.getValue().notifyClient(message);
            }catch (Exception e){
                idsOfClienteFailed.add(entry.getKey());
            }
        }
        idsOfClienteFailed.forEach(observers::remove);
    }
}
