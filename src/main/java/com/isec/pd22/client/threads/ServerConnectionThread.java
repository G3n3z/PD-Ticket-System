package services.NetworkService;

import enums.ENetworkMessageType;
import models.ConnectionModel;
import models.MessageModel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.function.BiFunction;

public class NetworkServiceClientTcp extends NetworkService<Socket, MessageModel>{
    private final ConnectionModel serverConnection;

    public NetworkServiceClientTcp(
            String hostname,
            int port,
            BiFunction<MessageModel, NetworkService<Socket, MessageModel>, MessageModel> onMessageProcessed
    ) throws IOException {
        super(new Socket(hostname, port), onMessageProcessed);

        serverConnection = new ConnectionModel(socket);

        initThreads();
    }

    public void sendMessage(MessageModel message) throws IOException {
        super.sendMessage(message, serverConnection.getObjOutputStream());
    }

    @Override
    protected MessageModel waitMessage() throws IOException, ClassNotFoundException {
        ObjectInputStream inputStream = serverConnection.getObjInputStream();

        return (MessageModel) inputStream.readObject();
    }

    @Override
    protected MessageModel waitMessage(ConnectionModel connection) {
        return null;
    }

    @Override
    protected void messagesReaderRoutine() {
        while (true) {
            try {
                MessageModel messageReceived = waitMessage();

                MessageModel messageToSend = this.onMessageProcessed.apply(messageReceived, this);

                if (messageToSend != null) {
                    sendMessage(messageToSend);
                }
            }
            catch (IOException e) {
                onMessageProcessed.apply(new MessageModel(ENetworkMessageType.CONNECTION_LOST), this);
                return;
            }
            catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void messagesReaderRoutine(ConnectionModel connection) { }

    @Override
    protected void waitConnection() {
    }

    private void initThreads() {
        new Thread(this::messagesReaderRoutine).start();
    }
}
