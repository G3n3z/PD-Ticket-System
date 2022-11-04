package com.isec.pd22.server.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Lugar {

    int id;
    String fila;
    String assento;
    double preco;
    int espetaculo_id;

    public static Lugar mapToEntity(ResultSet res) {
        Lugar lugar = new Lugar();
        try {
            lugar.setId(res.getInt("id"));
            lugar.setFila(res.getString("fila"));
            lugar.setAssento(res.getString("assento"));
            lugar.setPreco(res.getDouble("preco"));
            lugar.setEspetaculo_id(res.getInt("espetaculo_id"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return lugar;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
