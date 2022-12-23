package com.isec.pd22.rmi;

import com.isec.pd22.payload.HeartBeat;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerRemoteInterface extends Remote {

    boolean registerToNotifications(String uuid, ClientRemoteInterface clientRemote) throws RemoteException;

    boolean unregisterToNotifications(String uuid, ClientRemoteInterface clientRemote) throws RemoteException;

    RmiServerMessage getListOfServers() throws RemoteException;
}
