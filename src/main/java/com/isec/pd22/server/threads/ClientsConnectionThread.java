package com.isec.pd22.server.threads;

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

    public ClientsConnectionThread(InternalInfo internalInfo) throws SocketException {
        socket = new DatagramSocket(internalInfo.getPortUdp());
    }

    public DatagramSocket getSocket() {
        return socket;
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

    protected PackageModel waitMessage() throws IOException, ClassNotFoundException {
        DatagramPacket datagramPacketReceive = new DatagramPacket(
                new byte[ClientConnectionPayload.MAX_PAYLOAD_BYTES],
                ClientConnectionPayload.MAX_PAYLOAD_BYTES
        );

        socket.receive(datagramPacketReceive);

        ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(datagramPacketReceive.getData());
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayOutputStream);

        return new PackageModel((ClientConnectionPayload) inputStream.readObject(), datagramPacketReceive);
    }

    protected void messagesReaderRoutine() {
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

                ClientConnectionPayload payload = this.onMessageProcessed(packedMessage);

                if (payload != null) {
                    sendMessage(new PackageModel(payload, packedMessage.getPacket()));
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ClientConnectionPayload onMessageProcessed(PackageModel packedMessage) {



        return null;
    }
}
