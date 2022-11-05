package com.isec.pd22.client.models;

import com.isec.pd22.client.Client;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;

public class ModelManager {

    public static final String PROP_STATUS = "STATUS";
    public static final String PROP_ESPETACULOS = "ESPECTACULOS";
    public static final String ERROR_CONNECTION = "ERROR_CONNECTION";
    public static final String BAD_REQUEST = "BAD_REQUEST";

    public static final String ACTION_COMPLETE = "ACTION_COMPLETE";
    PropertyChangeSupport pcs;
    private StatusClient statusClient;

    Client client;

    Data data;

    public ModelManager() {
        pcs = new PropertyChangeSupport(this);
        statusClient = StatusClient.NOT_LOGGED;
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener listener){
        pcs.addPropertyChangeListener(property, listener);
    }

    public StatusClient getStatusClient() {
        return statusClient;
    }

    public void setStatusClient(StatusClient statusClient) {
        this.statusClient = statusClient;
        pcs.firePropertyChange(PROP_STATUS, null,null);
    }

    public void startServices(String [] args) {
        try {
            client = new Client(data, this, args);
        } catch (IOException | ClassNotFoundException |RuntimeException e) {
            pcs.firePropertyChange(ERROR_CONNECTION, null, null);
        }
    }

    public void receivedEspetaculos(){
        pcs.firePropertyChange(PROP_ESPETACULOS, null, null);
    }

    public void receivedLoginResponse(StatusClient statusClient){
        this.statusClient = statusClient;
        pcs.firePropertyChange(PROP_STATUS, null, null);
    }

    public void sendMessage(ClientMSG msg) {
        try {
            client.sendMessage(msg);
        } catch (IOException e) {
            AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "", "Não foi possivel fazer o pedido ao servidor");
            AlertSingleton.getInstanceWarning().showAndWait();
        }
    }

    public void setErrorConnection() {
        pcs.firePropertyChange(ERROR_CONNECTION, null, null);
    }

    public void badRequest(ClientMSG msg){
        pcs.firePropertyChange(BAD_REQUEST, null, null);
    }

    public void registerCompleted() {
        pcs.firePropertyChange(ACTION_COMPLETE, null, null);


    }
}
