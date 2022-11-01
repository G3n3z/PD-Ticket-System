package com.isec.pd22.server.models;

import com.isec.pd22.payload.ClientConnectionPayload;

import java.net.DatagramPacket;

public class PackageModel {
    private final ClientConnectionPayload payload;

    private final DatagramPacket packet;

    public PackageModel(ClientConnectionPayload payload, DatagramPacket receivedPacket) {
        this.payload = payload;
        this.packet = receivedPacket;
    }

    public ClientConnectionPayload getPayload() {
        return payload;
    }

    public DatagramPacket getPacket() {
        return packet;
    }
}
