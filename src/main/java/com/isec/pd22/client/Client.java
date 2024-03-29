package com.isec.pd22.client;

import com.isec.pd22.client.models.ClientModel;
import com.isec.pd22.client.models.ConnectionModel;
import com.isec.pd22.client.models.Data;
import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.threads.ServerConnectionThread;
import com.isec.pd22.enums.*;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.ServersRequestPayload;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.tcp.Request.Reconnect;
import com.isec.pd22.utils.UdpUtils;

import java.io.IOException;
import java.net.*;

public class Client {
    private static final int REQUESTS_FOR_SERVERS_ATTEMPTS = 3;
    private ClientModel clientModel;
    private Data data;
    private ModelManager modelManager;

    private boolean isClosingConnection = false;

    public Client(Data data, ModelManager modelManager, String[] args) throws RuntimeException, IOException, ClassNotFoundException {
        clientModel = new ClientModel(args);
        this.data = data;
        this.modelManager = modelManager;

        requestServers();

        Socket tcpServerSocket = connectToServer();
        initServerThread(tcpServerSocket);
    }

    public void sendMessage(ClientMSG msg) throws IOException {
        clientModel.getServerConnectionThread().sendMessage(msg);
    }

    public void closeConnection() {
        isClosingConnection = true;

        try {
            clientModel.getTcpServerConnection().getSocket().close();
        } catch (IOException ignored) {}
    }

    private void initServerThread(Socket tcpServerSocket) throws IOException {
        clientModel.setTcpServerConnection(new ConnectionModel(tcpServerSocket));
        clientModel.setServerConnectionThread(new ServerConnectionThread(clientModel.getTcpServerConnection(), this::onMessageReceived));

        clientModel.getServerConnectionThread().start();
    }



    private void onMessageReceived(ClientMSG mensage, ServerConnectionThread service) {
        System.out.println("Mensagem recebida " + mensage.getClientsPayloadType());

        modelManager.setLastMessage(mensage);

        switch (mensage.getClientsPayloadType()) {
            case CONNECTION_LOST -> {
                if (!isClosingConnection) {
                    reestablishNewServerConnection();
                    System.out.println("Restablished");
                    break;
                }

                System.out.println("Conexão Terminada, adeus!!");
            }
            case USER_REGISTER -> {
                if (modelManager.getStatusClient() == StatusClient.REGISTER){
                    modelManager.registerCompleted();
                }
            }
            case BAD_REQUEST -> {
                modelManager.badRequest(mensage);
            }
            case ACTION_SUCCEDED -> modelManager.actionSuccess(mensage);
            case LOGGED_IN -> {
                data.setUser(mensage.getUser());
                if(mensage.getUser().getRole() == Role.USER){
                    modelManager.setStatusClient(StatusClient.USER);
                }else {
                    modelManager.setStatusClient(StatusClient.ADMIN);
                }

            }
            case LOGOUT -> modelManager.logout();
            case FILE_UPDATED -> modelManager.fileUploaded();
            case PART_OF_FILE_UPLOADED -> System.out.println("Parte do ficheiro carregado");
            case CONSULT_SPECTACLE -> modelManager.fireEspetaculos(mensage);
            case RESERVAS_RESPONSE -> modelManager.fireReservasAdmin(mensage);
            case SPECTACLE_DETAILS -> modelManager.fireEspectaculo(mensage);
            case TRY_LATER -> modelManager.tryLater();
            case SUBMIT_RESERVATION_NOT_PAYED -> modelManager.reservationWaitingPay(mensage);
            case SUBMIT_RESERVATION_COMPLETE -> modelManager.reservationComplete(mensage);
            case DELETE_SPECTACLE -> {
                modelManager.removeSpectacle(mensage);
            }
            case NOT_AUTHENTICATED -> modelManager.notAuhtenticated(mensage);
            case SHUTDOWN -> {
                Reconnect msg = (Reconnect) mensage;
                modelManager.setLastSubscription(msg.getSubscription());
                clientModel.setServersList(msg.getServerList());
            }
        }

    }

    private void requestServers() throws IOException, ClassNotFoundException {
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(2000);

        DatagramPacket packetToSend = new DatagramPacket(
                new byte[ServersRequestPayload.MAX_PAYLOAD_BYTES],
                ServersRequestPayload.MAX_PAYLOAD_BYTES,
                InetAddress.getByName(clientModel.getUdpServerIp()),
                clientModel.getUdpServerPort()
        );

        for (int i = 1; i <= REQUESTS_FOR_SERVERS_ATTEMPTS; i++) {
            try {
                UdpUtils.sendObject(socket, packetToSend, new ServersRequestPayload(ClientsPayloadType.REQUEST_SERVERS));
                ServersRequestPayload serverResponse = UdpUtils.receiveObject(socket);

                clientModel.setServersList(serverResponse.getServersListCollection());

                socket.close();

                return;
            } catch (SocketTimeoutException e) {
                System.out.println("[requestServers timeout]");
            }
        }

        // Retorna exceção caso nenhuma resposta seja obtida
        throw new RuntimeException("Não foi possivel obter uma resposta com os servidores disponiveis.");
    }

    private Socket connectToServer() {
        for (HeartBeat serverDetails: clientModel.getServersList()) {
            try {
                System.out.print("\nConexão com: " + serverDetails.getIp() + ":" + serverDetails.getPortTcpClients());

                return new Socket(serverDetails.getIp(), serverDetails.getPortTcpClients());
            } catch (IOException e) {
                // Conexão servidor falhou, processa exceção e avança para a proxima iteração
                System.out.println("...FALHOU");
            }
        }

        // Retorna exceção caso nenhuma conexão seja estabelecida
        throw new RuntimeException("Não foi possivel estabelecer ligação a nenhum servidor.");
    }

    private void reestablishNewServerConnection() {
        try {
            Socket serverSocket = connectToServer();

            initServerThread(serverSocket);
            sendReconnectSubscription();

        } catch (RuntimeException e) {
            System.err.println(e);
            modelManager.setErrorConnection();
        } catch (IOException ignored) { }
    }

    private void sendReconnectSubscription() throws IOException {
        if (modelManager.getStatusClient() == StatusClient.USER || modelManager.getStatusClient() == StatusClient.ADMIN){
            if(modelManager.getLastSubscription() != null){
                Reconnect msg = new Reconnect(ClientActions.RECONNECT);
                msg.setSubscription(modelManager.getLastSubscription());
                msg.setUser(modelManager.getUser());
                sendMessage(msg);
            }
        }
    }
}
