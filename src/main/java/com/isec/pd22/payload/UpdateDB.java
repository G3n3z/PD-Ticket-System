package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class UpdateDB extends MulticastMSG{
    int numVersion;

    String ipDest;

    int portUdpDest;
    String ip;
    int portTCP;

    public UpdateDB(TypeOfMulticastMsg type, int numVersion, String ip, int portTCP) {
        super(type);
        this.numVersion = numVersion;
        this.ip = ip;
        this.portTCP = portTCP;
    }

    public int getNumVersion() {
        return numVersion;
    }

    public void setNumVersion(int numVersion) {
        this.numVersion = numVersion;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPortTCP() {
        return portTCP;
    }

    public void setPortTCP(int portTCP) {
        this.portTCP = portTCP;
    }

    public String getIpDest() {
        return ipDest;
    }

    public void setIpDest(String ipDest) {
        this.ipDest = ipDest;
    }

    public int getPortUdpDest() {
        return portUdpDest;
    }

    public void setPortUdpDest(int portUdpDest) {
        this.portUdpDest = portUdpDest;
    }
}
