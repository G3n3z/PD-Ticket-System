package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Espetaculo;

import java.util.ArrayList;
import java.util.List;

public class Espetaculos extends ClientMSG {
    List<Espetaculo> espetaculos ;

    public Espetaculos(ClientActions action, List<Espetaculo> espetaculos) {
        super(action);
        this.espetaculos = espetaculos;
    }

    public Espetaculos(List<Espetaculo> espetaculos) {
        this.espetaculos = espetaculos;
    }

    public Espetaculos(ClientsPayloadType clientsPayloadType) {
        super(clientsPayloadType);
    }

    public Espetaculos(ClientActions action) {
        super(action);
    }

    public Espetaculos(ClientsPayloadType clientsPayloadType, List<Espetaculo> espetaculos) {
        super(clientsPayloadType);
        this.espetaculos = espetaculos;
    }

    public List<Espetaculo> getEspetaculos() {
        return espetaculos;
    }

    public void setEspetaculos(List<Espetaculo> espetaculos) {
        this.espetaculos = espetaculos;
    }
}
