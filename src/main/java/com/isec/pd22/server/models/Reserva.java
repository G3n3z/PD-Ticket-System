package com.isec.pd22.server.models;

import com.isec.pd22.enums.Payment;
import com.isec.pd22.utils.Constants;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class Reserva {

    int idReserva;
    Date data_hora;
    Payment payment;
    int idUser;
    int idEspectaculo;

    public static Reserva mapToEntity(ResultSet res) {
        Reserva reserva = new Reserva();
        try {
            reserva.idReserva = res.getInt("id");
            reserva.data_hora = Constants.stringToDate(res.getString("data_hora")) ;
            reserva.payment = Payment.fromInteger(res.getInt("pago"));
            reserva.idUser = res.getInt("id_utilizador");
            reserva.idEspectaculo = res.getInt("id_espetaculo");
        } catch (ParseException | SQLException e) {
        }
        return reserva;
    }

    public int getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(int idReserva) {
        this.idReserva = idReserva;
    }

    public Date getData_hora() {
        return data_hora;
    }

    public void setData_hora(Date data_hora) {
        this.data_hora = data_hora;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public int getIdEspectaculo() {
        return idEspectaculo;
    }

    public void setIdEspectaculo(int idEspectaculo) {
        this.idEspectaculo = idEspectaculo;
    }
}
