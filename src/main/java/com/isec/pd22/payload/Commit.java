package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class Commit extends MulticastMSG{

    String ip; int portUdp;
    public Commit(TypeOfMulticastMsg typeMsg) {
        super(typeMsg);
    }

    public Commit(TypeOfMulticastMsg typeMsg, String ip, int portUdp) {
        super(typeMsg);
        this.ip = ip;
        this.portUdp = portUdp;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortUdp() {
        return portUdp;
    }

    public void setPortUdp(int portUdp) {
        this.portUdp = portUdp;
    }
}
