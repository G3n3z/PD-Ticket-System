package com.isec.pd22.client.models;

import com.isec.pd22.client.Client;
import com.isec.pd22.client.threads.SendFile;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.client.ui.utils.ModalIndicator;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.ListPlaces;
import com.isec.pd22.payload.tcp.Request.RequestDetailsEspetaculo;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Reserva;
import com.isec.pd22.server.models.User;
import javafx.stage.Stage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ModelManager {

    public static final String PROP_STATUS = "STATUS";
    public static final String PROP_ESPETACULOS = "ESPECTACULOS";
    public static final String ERROR_CONNECTION = "ERROR_CONNECTION";
    public static final String BAD_REQUEST = "BAD_REQUEST";

    public static final String ACTION_COMPLETE = "ACTION_COMPLETE";
    public static final String LOGOUT = "LOGOUT";
    public static final String FILE_UPDATED = "FILE_UPLOADED";
    public static final String ALL_ESPETACULOS = "ALL_ESPETACULOS";
    public static final String PROP_RESERVAS = "PROP_RESERVAS";
    public static final String PROP_ESPETACULO_DETAILS = "PROP_ESPETACULO_DETAILS";
    public static final String PROP_TRY_LATER = "TRY_LATER";
    public static final String EDIT_USER = "EDIT_USER";
    public static final String PROP_ESPETACULO_DETAILS_WAITING_PAYMENT = "PROP_ESPETACULO_DETAILS_WAITING_PAYMENT";
    public static final String PROP_CLOSE_ALERT = "PROP_CLOSE_ALERT";
    public static final String PROP_DELETED_SPECPTACLE = "PROP_DELETED_SPECPTACLE";
    PropertyChangeSupport pcs;
    private StatusClient statusClient;

    Client client;

    Data data;
    ModalIndicator modalIndicator = null;

    public ModelManager() {
        pcs = new PropertyChangeSupport(this);
        statusClient = StatusClient.NOT_LOGGED;
        data = new Data();
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
       // modalIndicator = new ModalIndicator(this);
        try {
            client.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
            AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "", "Não foi possivel fazer o pedido ao servidor")
                    .showAndWait();
        }
    }

    public void setErrorConnection() {
        pcs.firePropertyChange(ERROR_CONNECTION, null, null);
    }

    public void badRequest(ClientMSG msg){
        pcs.firePropertyChange(BAD_REQUEST, null, msg.getMessage());
    }

    public void registerCompleted() {
        pcs.firePropertyChange(ACTION_COMPLETE, null, null);
    }

    public User getUser() {
       return data.getUser();
    }

    public void logout() {
        pcs.firePropertyChange(LOGOUT, null, null);
    }

    public void sendFile(File f) {
        SendFile sendFile = new SendFile(this, f);
        sendFile.start();
    }

    public void notErrorUpdatingFile() {
    }

    public void fileUploaded() {
        pcs.firePropertyChange(FILE_UPDATED, null, null);
    }

    public List<Espetaculo> getEspectaculos() {
        return data.espetaculos;
    }

    public void fireEspetaculos(ClientMSG mensage) {
        Espetaculos e = (Espetaculos) mensage;
        data.espetaculos = e.getEspetaculos();
        pcs.firePropertyChange(ALL_ESPETACULOS, null, null);
    }

    public List<Reserva> getReservas() {
        return data.getReservas();
    }

    public void fireReservasAdmin(ClientMSG mensage) {
        RequestListReservas reservas = (RequestListReservas) mensage;
        if(mensage.getAction() == ClientActions.CONSULT_PAYED_RESERVATION){
            data.setReservasPayed(reservas.getReservas());
        }else{
            data.setReservas(reservas.getReservas());
        }
        pcs.firePropertyChange(PROP_RESERVAS, null, null);
    }

    public Espetaculo getEspectaculo() {
        return data.espetaculo;
    }

    public void fireEspectaculo(ClientMSG mensage) {
        RequestDetailsEspetaculo r = (RequestDetailsEspetaculo) mensage;
        if (r.getEspetaculo() != null ) {
            if (r.getEspetaculo().getVisivel() == 0 && statusClient == StatusClient.USER){
                setLastMessage(new ClientMSG(ClientActions.CONSULT_SPECTACLE, ClientsPayloadType.BAD_REQUEST, "Espetaculo deixou de ser visivel"));
                pcs.firePropertyChange(BAD_REQUEST, null, null);
                return;
            }
            data.espetaculo = r.getEspetaculo();
            pcs.firePropertyChange(PROP_ESPETACULO_DETAILS, null, null);
        }
        else{
            pcs.firePropertyChange(PROP_DELETED_SPECPTACLE, null, null);
        }

    }

    public void clearData() {
        data.clear();
    }

    public List<Reserva> getReservasPayed() {
        return data.getReservasPayed();
    }

    public void tryLater() {
        pcs.firePropertyChange(PROP_TRY_LATER, null, null);
    }

//    public void startIndicator(Stage stage) {
//        modalIndicator = new ModalIndicator(this);
//        modalIndicator.show(stage);
//    }

    public void actionSuccess(ClientMSG mensage) {
        pcs.firePropertyChange(ACTION_COMPLETE,null,null);
    }

    public void editUser(){
        pcs.firePropertyChange(EDIT_USER,null,null);
    }

    public void addReservations(ClientMSG mensage){
        ListPlaces listPlaces = (ListPlaces) mensage;
        listPlaces.getPlaces().forEach(data.getEspetaculo().getLugares()::remove);
        data.getEspetaculo().getLugares().addAll(listPlaces.getPlaces());
    }
    public void reservationComplete(ClientMSG mensage) {
        addReservations(mensage);
        closeAlert();
        pcs.firePropertyChange(PROP_ESPETACULO_DETAILS, null, null);
    }
    public void closeAlert(){
        pcs.firePropertyChange(PROP_CLOSE_ALERT, null, null);
    }
    public void reservationWaitingPay(ClientMSG mensage) {
        addReservations(mensage);
        pcs.firePropertyChange(PROP_ESPETACULO_DETAILS, null, null);
        pcs.firePropertyChange(PROP_ESPETACULO_DETAILS_WAITING_PAYMENT, null, null);
    }

    public void closeConnection() {
        client.closeConnection();
    }

    public void setLastMessage(ClientMSG mensage) {
        data.setLastMessage(mensage);
    }

    public ClientMSG getLastMessage() {
        return data.getMessage();
    }

    public void removeSpectacle(ClientMSG mensage) {
        RequestDetailsEspetaculo espetaculo = (RequestDetailsEspetaculo) mensage;
        data.getEspetaculos().removeIf(espetaculo1 -> espetaculo.getEspetaculo()!= null &&
                espetaculo1.getIdEspetaculo() == espetaculo.getEspetaculo().getIdEspetaculo());
        pcs.firePropertyChange(ALL_ESPETACULOS, null, null);
    }

    public void notAuhtenticated(ClientMSG mensage) {
        pcs.firePropertyChange(BAD_REQUEST, null, null);
    }
}
