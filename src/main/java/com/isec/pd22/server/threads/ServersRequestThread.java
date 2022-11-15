package com.isec.pd22.server.threads;

import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.ServersRequestPayload;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.PackageModel;
import com.isec.pd22.utils.UdpUtils;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class ServersRequestThread extends Thread{
    private final LinkedList<PackageModel> messagesQueue = new LinkedList<>();
    private final Semaphore semaphoreQueue = new Semaphore(0);
    private final DatagramSocket socket;
    private final InternalInfo internalInfo;

    private boolean toClose = false;

    public ServersRequestThread(DatagramSocket socket, InternalInfo internalInfo) {
        this.socket = socket;
        this.internalInfo = internalInfo;

        new Thread(this::messagesQueueThreadRoutine).start();
    }

    @Override
    public void run() {
        this.messagesReaderRoutine();
    }

    private void sendMessage(PackageModel packagedMessageModel) throws IOException {
        try {
            UdpUtils.sendObject(socket, packagedMessageModel.getPacket(), packagedMessageModel.getPayload());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw e;
        }
    }

    private PackageModel waitMessage() throws IOException, ClassNotFoundException {
        DatagramPacket datagramPacketReceive = new DatagramPacket(
                new byte[ServersRequestPayload.MAX_PAYLOAD_BYTES],
                ServersRequestPayload.MAX_PAYLOAD_BYTES
        );

        ServersRequestPayload payloadReceived = UdpUtils.receiveObject(socket, datagramPacketReceive);

        return new PackageModel(payloadReceived, datagramPacketReceive);
    }

    private void messagesReaderRoutine() {
        while (true) {
            try {
                PackageModel packageReceived = waitMessage();

                messagesQueue.add(packageReceived);
                semaphoreQueue.release(1);
            }
            catch (ClassNotFoundException e) {
                System.out.println("ERRO: waitMessage não consegui converter o objeto para o tipo certo.");
            }
            catch (IOException e) {
                System.out.println(e.getMessage());

                toClose = true;
                semaphoreQueue.release(1);
                break;
            }
        }
    }

    private void messagesQueueThreadRoutine() {
        while (true) {
            try {
                semaphoreQueue.acquire(1);

                if (toClose){
                    break;
                }

                PackageModel packedMessage = messagesQueue.pop();

                this.onMessageProcessed(packedMessage);

            } catch (InterruptedException | IOException e) {
                System.out.println(e.getMessage());
                break;
            }
        }
    }

    private void onMessageProcessed(PackageModel packedMessage) throws IOException {
        ServersRequestPayload payload = new ServersRequestPayload(internalInfo.getOrderedHeatBeats(), ClientsPayloadType.REQUEST_SERVERS_SUCCESS);

        sendMessage(new PackageModel(payload, packedMessage.getPacket()));
    }
}
