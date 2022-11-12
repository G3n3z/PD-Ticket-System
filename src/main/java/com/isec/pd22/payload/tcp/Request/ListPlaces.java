package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Lugar;

import java.util.List;

public class ListPlaces extends ClientMSG
{
    private List<Lugar> places;

    public ListPlaces(ClientActions submitReservation) {
        super(submitReservation);
    }

    public ListPlaces(ClientActions action, ClientsPayloadType clientsPayloadType, List<Lugar> places) {
        super(action, clientsPayloadType);
        this.places = places;
    }

    public ListPlaces(ClientsPayloadType clientsPayloadType, List<Lugar> places) {
        super(clientsPayloadType);
        this.places = places;
    }

    public ListPlaces() {
    }

    public List<Lugar> getPlaces() {
        return places;
    }

    public void setPlaces(List<Lugar> places) {
        this.places = places;
    }
}
