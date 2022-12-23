package com.isec.pd22.rmi;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RmiServerMessage implements Serializable {

    @Serial
    private static final long serialVersionUID= 1L;
    List<RmiServidor> servers;

    public RmiServerMessage() {

    }

    public List<RmiServidor> getServers() {
        return servers;
    }

    public void setServers(List<RmiServidor> servers) {
        this.servers = servers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Servidores\n");
        servers.forEach(sb::append);
        return sb.toString();
    }
}
