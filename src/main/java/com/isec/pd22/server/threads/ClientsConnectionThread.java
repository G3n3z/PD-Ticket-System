package com.isec.pd22.server.threads;

import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.ClientConnectionPayload;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.PackageModel;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class ClientsConnectionThread extends Thread{
    private final LinkedList<PackageModel> messagesQueue = new LinkedList<>();
    private final Semaphore semaphoreQueue = new Semaphore(0);
    private final DatagramSocket socket;
    private final InternalInfo internalInfo;

    public ClientsConnectionThread(DatagramSocket socket, InternalInfo internalInfo) {
        this.socket = socket;
        this.internalInfo = internalInfo;
    }

    @Override
    public void run() {
        this.messagesReaderRoutine();
        new Thread(this::messagesQueueThreadRoutine).start();
    }

    public void sendMessage(PackageModel packagedMessageModel) {
        try {
            DatagramPacket packet = packagedMessageModel.getPacket();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

            outputStream.writeObject(packagedMessageModel.getPayload());

            byte[] byteStreamMessage = byteArrayOutputStream.toByteArray();

            packet.setData(byteStreamMessage);
            packet.setLength(byteStreamMessage.length);

            socket.send(packet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private PackageModel waitMessage() throws IOException, ClassNotFoundException {
        DatagramPacket datagramPacketReceive = new DatagramPacket(
                new byte[ClientConnectionPayload.MAX_PAYLOAD_BYTES],
                ClientConnectionPayload.MAX_PAYLOAD_BYTES
        );

        socket.receive(datagramPacketReceive);

        ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(datagramPacketReceive.getData());
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayOutputStream);

        return new PackageModel((ClientConnectionPayload) inputStream.readObject(), datagramPacketReceive);
    }

    private void messagesReaderRoutine() {
        while (true) {
            try {
                PackageModel packageReceived = waitMessage();

                messagesQueue.add(packageReceived);
                semaphoreQueue.release(1);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void messagesQueueThreadRoutine() {
        while (true) {
            try {
                semaphoreQueue.acquire(1);

                PackageModel packedMessage = messagesQueue.pop();

                this.onMessageProcessed(packedMessage);

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onMessageProcessed(PackageModel packedMessage) {
        ClientConnectionPayload payload = new ClientConnectionPayload(internalInfo.getHeatBeats(), ClientsPayloadType.REQUEST_SERVERS_SUCCESS);

        sendMessage(new PackageModel(payload, packedMessage.getPacket()));
    }
}
