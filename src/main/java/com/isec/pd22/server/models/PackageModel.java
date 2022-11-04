package com.isec.pd22.server.models;

import com.isec.pd22.payload.ServersRequestPayload;

import java.net.DatagramPacket;

public class PackageModel {
    private final ServersRequestPayload payload;

    private final DatagramPacket packet;

    public PackageModel(ServersRequestPayload payload, DatagramPacket receivedPacket) {
        this.payload = payload;
        this.packet = receivedPacket;
    }

    public ServersRequestPayload getPayload() {
        return payload;
    }

    public DatagramPacket getPacket() {
        return packet;
    }
}
