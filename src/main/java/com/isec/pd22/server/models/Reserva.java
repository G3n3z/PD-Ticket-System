package com.isec.pd22.server.models;

import com.isec.pd22.enums.Payment;
import com.isec.pd22.utils.Constants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class Reserva {

    int id;
    Date data_hora;
    Payment payment;
    int idUser;
    int idEspectaculo;

    public static Reserva mapToEntity(ResultSet res) {
        Reserva reserva = new Reserva();
        try {
            reserva.id = res.getInt("id");
            reserva.data_hora = Constants.stringToDate(res.getString("data_hora")) ;
            reserva.payment = Payment.fromInteger(res.getInt("pago"));
            reserva.idUser = res.getInt("id_utilizador");
            reserva.idEspectaculo = res.getInt("id_espetaculo");
        } catch (ParseException | SQLException e) {
        }
        return reserva;
    }


}