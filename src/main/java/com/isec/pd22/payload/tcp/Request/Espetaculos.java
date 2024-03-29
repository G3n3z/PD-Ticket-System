package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.Espetaculo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Espetaculos extends ClientMSG {
    List<Espetaculo> espetaculos ;
    Map<String,String> filtros;

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
        filtros = new HashMap<>();
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

    public Map<String, String> getFiltros() {
        return filtros;
    }

    public void setFiltros(Map<String, String> filtros) {
        this.filtros = filtros;
    }
}
