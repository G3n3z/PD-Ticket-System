package com.isec.pd22.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MonitorRmiRemote extends UnicastRemoteObject implements ClientRemoteInterface {

    public MonitorRmiRemote() throws RemoteException {}

    public String notifyClient(String msg) throws RemoteException{
        System.out.println(msg);
        return msg;
    }

}
