package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Lugar;

import java.util.List;

public class RequestDetailsEspetaculo extends ClientMSG {

    Espetaculo espetaculo;
    List<Lugar> lugars;

    public RequestDetailsEspetaculo(ClientActions action) {
        super(action);
    }

    public Espetaculo getEspetaculo() {
        return espetaculo;
    }

    public void setEspetaculo(Espetaculo espetaculo) {
        this.espetaculo = espetaculo;
    }

    public List<Lugar> getLugars() {
        return lugars;
    }

    public void setLugars(List<Lugar> lugars) {
        this.lugars = lugars;
    }
}
