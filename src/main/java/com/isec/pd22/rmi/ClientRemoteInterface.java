package com.isec.pd22.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRemoteInterface extends Remote {

    String notifyClient(String msg)throws RemoteException;

}
