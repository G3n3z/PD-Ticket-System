package com.isec.pd22.client.models;

import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Reserva;
import com.isec.pd22.server.models.User;

import java.util.ArrayList;
import java.util.List;

public class Data {
    private List<Reserva> reservas = new ArrayList<>();
    private List<Reserva> reservasPayed = new ArrayList<>();
    Espetaculo espetaculo;
    List<Espetaculo> espetaculos = new ArrayList<>();
    User user;
    private ClientMSG lastMessage;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }

    public List<Espetaculo> getEspetaculos() {
        return espetaculos;
    }

    public void setEspetaculos(List<Espetaculo> espetaculos) {
        this.espetaculos = espetaculos;
    }

    public void clear() {
        espetaculos = new ArrayList<>();
        espetaculo = null;
        reservas = new ArrayList<>();
        user = null;
        reservasPayed = new ArrayList<>();
    }

    public List<Reserva> getReservasPayed() {
        return reservasPayed;
    }

    public void setReservasPayed(List<Reserva> reservasPayed) {
        this.reservasPayed = reservasPayed;
    }

    public Espetaculo getEspetaculo() {
        return espetaculo;
    }

    public void setLastMessage(ClientMSG lastMessage) {
        this.lastMessage = lastMessage;
    }

    public ClientMSG getMessage() {
        return lastMessage;
    }
}
