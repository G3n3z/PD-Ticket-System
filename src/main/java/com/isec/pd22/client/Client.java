package com.isec.pd22.client;

import com.isec.pd22.client.models.ClientModel;
import com.isec.pd22.client.models.ConnectionModel;
import com.isec.pd22.client.threads.ServerConnectionThread;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.payload.ServersRequestPayload;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.utils.UdpUtils;

import java.io.IOException;
import java.net.*;

public class Client {
    private static final int REQUESTS_FOR_SERVERS_ATTEMPTS = 3;
    private static ClientModel clientModel;

    public static void main(String[] args) throws RuntimeException, IOException, ClassNotFoundException {
        clientModel = new ClientModel(args);

        requestServers();

        Socket tcpServerSocket = connectToServer();

        clientModel.setTcpServerConnection(new ConnectionModel(tcpServerSocket));
        clientModel.setServerConnectionThread(new ServerConnectionThread(clientModel.getTcpServerConnection(), Client::onMessageReceived));
        clientModel.getServerConnectionThread().start();


        // TODO implementar sistema de processamento de comandos
    }

    // TODO interpretar as mensagens recebidas do servidor aqui!
    private static void onMessageReceived(ClientMSG mensage, ServerConnectionThread service) {
        switch (mensage.getClientsPayloadType()) {
            case CONNECTION_LOST -> reestablishNewServerConnection();
        }
    }

    private static void requestServers() throws IOException, ClassNotFoundException {
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

    private static Socket connectToServer() {
        for (HeartBeat serverDetails: clientModel.getServersList()) {
            try {
                System.out.print("A tentar ligar a: " + serverDetails.getIp() + ":" + serverDetails.getPortTcpClients() + "...");

                return new Socket(serverDetails.getIp(), serverDetails.getPortTcpClients());
            } catch (IOException e) {
                // Conexão servidor falhou, processa exceção e avança para a proxima iteração
                System.out.println("FALHOU");
            }
        }

        // Retorna exceção caso nenhuma conexão seja estabelecida
        throw new RuntimeException("Não foi possivel estabelecer ligação a nenhum servidor.");
    }

    private static void reestablishNewServerConnection() {
        try {
            Socket serverSocket = connectToServer();
            ConnectionModel serverConnection = new ConnectionModel(serverSocket);

            clientModel.setTcpServerConnection(serverConnection);
            clientModel.getServerConnectionThread().setConnection(serverConnection);
        } catch (RuntimeException e) {
            System.err.println(e);
            System.exit(0);
        } catch (IOException ignored) { }
    }
}
