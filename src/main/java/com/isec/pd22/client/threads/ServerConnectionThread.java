package com.isec.pd22.client.threads;

import com.isec.pd22.client.models.ConnectionModel;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.tcp.ClientMSG;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.function.BiConsumer;

public class ServerConnectionThread extends Thread {
    private ConnectionModel connection;
    private final BiConsumer<ClientMSG, ServerConnectionThread> onMessageProcessed;

    public ServerConnectionThread(
            ConnectionModel connection,
            BiConsumer<ClientMSG, ServerConnectionThread> onMessageProcessed
    ) {
        this.connection = connection;
        this.onMessageProcessed = onMessageProcessed;
    }

    @Override
    public void run() {
        messagesReaderRoutine();
    }

    public void setConnection(ConnectionModel connection) {
        synchronized (this) {
            this.connection = connection;
        }
    }

    public void sendMessage(ClientMSG message) throws IOException {
        ObjectOutputStream outputStream = connection.getObjOutputStream();

        if (outputStream == null) { return; }

        outputStream.writeObject(message);
    }

    private ClientMSG waitMessage() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = connection.getObjInputStream();

        return (ClientMSG) inputStream.readObject();
    }

    private void messagesReaderRoutine() {
        while (true) {
            try {
                ClientMSG messageReceived = waitMessage();

                onMessageProcessed.accept(messageReceived, this);
            }
            catch (IOException e) {
                System.out.println(e);
                onMessageProcessed.accept(new ClientMSG(ClientsPayloadType.CONNECTION_LOST), this);
                break;
                //TODO:
            }
            catch (ClassNotFoundException e) {
                System.out.println("[ERRO] - o tipo do objeto enviado deve ser ClientMSG");
            }
        }
    }
}
