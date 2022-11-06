package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Reserva;

import java.util.List;

public class RequestListReservas extends ClientMSG {

    List<Reserva> reservas;


    public RequestListReservas(ClientActions action, List<Reserva> reservas) {
        super(action);
        this.reservas = reservas;
    }

    public RequestListReservas(ClientsPayloadType clientsPayloadType) {
        super(clientsPayloadType);
    }

    public RequestListReservas(ClientsPayloadType clientsPayloadType, List<Reserva> reservas) {
        super(clientsPayloadType);
        this.reservas = reservas;
    }

    public RequestListReservas(ClientActions action) {
        super(action);
    }

    public RequestListReservas(ClientsPayloadType clientsPayloadType, List<Reserva> reservas) {
        super(clientsPayloadType);
        this.reservas = reservas;
    }

    public List<Reserva> getReservas() {
        return reservas;
    }

    public void setReservas(List<Reserva> reservas) {
        this.reservas = reservas;
    }
}
