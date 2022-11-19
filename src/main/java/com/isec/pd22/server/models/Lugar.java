package com.isec.pd22.server.models;

import java.io.Serial;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Lugar implements Serializable, Comparable<Lugar> {
    @Serial
    private static final long serialVersionUID = 1L;
    int idLugar;
    String fila;
    String assento;
    double preco;
    int espetaculo_id;

    Reserva reserva;
    public Lugar() {
    }

    public Lugar(String fila, String assento, double preco) {
        this.fila = fila;
        this.assento = assento;
        this.preco = preco;
    }

    public static Lugar mapToEntity(ResultSet res) {
        Lugar lugar = new Lugar();
        try {
            lugar.setIdLugar(res.getInt("id"));
            lugar.setFila(res.getString("fila"));
            lugar.setAssento(res.getString("assento"));
            lugar.setPreco(res.getDouble("preco"));
            lugar.setEspetaculo_id(res.getInt("espetaculo_id"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lugar;
    }


    public int getIdLugar() {
        return idLugar;
    }

    public void setIdLugar(int idLugar) {
        this.idLugar = idLugar;
    }

    public String getFila() {
        return fila;
    }

    public void setFila(String fila) {
        this.fila = fila;
    }

    public String getAssento() {
        return assento;
    }

    public void setAssento(String assento) {
        this.assento = assento;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public int getEspetaculo_id() {
        return espetaculo_id;
    }

    public void setEspetaculo_id(int espetaculo_id) {
        this.espetaculo_id = espetaculo_id;
    }

    @Override
    public int compareTo(Lugar o) {
        return Integer.parseInt(assento.trim()) - Integer.parseInt(o.assento.trim());
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Reserva getReserva() {
        return reserva;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Lugar lugar = (Lugar) o;

        if (getEspetaculo_id() != lugar.getEspetaculo_id()) return false;
        if (!getFila().equals(lugar.getFila())) return false;
        return getAssento().equals(lugar.getAssento());
    }

    @Override
    public int hashCode() {
        int result = getFila().hashCode();
        result = 31 * result + getAssento().hashCode();
        result = 31 * result + getEspetaculo_id();
        return result;
    }
}
