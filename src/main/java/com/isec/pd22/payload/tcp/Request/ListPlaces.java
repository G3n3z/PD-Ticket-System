package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Lugar;

import java.util.List;

public class ListPlaces extends ClientMSG
{
    private List<Lugar> places;

    public List<Lugar> getPlaces() {
        return places;
    }

    public void setPlaces(List<Lugar> places) {
        this.places = places;
    }
}
