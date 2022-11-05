package com.isec.pd22.utils;

import com.isec.pd22.payload.ServersRequestPayload;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpUtils {

    public static <T> void sendObject(DatagramSocket socket, DatagramPacket packetToSend, T object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        outputStream.writeObject(object);

        byte[] byteStreamMessage = byteArrayOutputStream.toByteArray();

        packetToSend.setData(byteStreamMessage);
        packetToSend.setLength(byteStreamMessage.length);

        socket.send(packetToSend);
    }

    public static <T> T receiveObject(DatagramSocket socket) throws IOException, ClassNotFoundException {
        DatagramPacket packet = new DatagramPacket(
                new byte[ServersRequestPayload.MAX_PAYLOAD_BYTES],
                ServersRequestPayload.MAX_PAYLOAD_BYTES
        );

        return receiveObject(socket, packet);
    }

    public static <T> T receiveObject(DatagramSocket socket, DatagramPacket packet) throws IOException, ClassNotFoundException {
        socket.receive(packet);

        ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayOutputStream);

        return (T) inputStream.readObject();
    }

}
