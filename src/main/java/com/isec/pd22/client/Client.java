package com.isec.pd22.client;

import com.isec.pd22.client.models.ClientModel;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.payload.ClientConnectionPayload;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.utils.UdpUtils;

import java.io.IOException;
import java.net.*;

public class Client {

    private static ClientModel clientModel;

    public static void main(String[] args) throws RuntimeException, IOException, ClassNotFoundException {
        clientModel = new ClientModel(args);

        requestServers();

        Socket serverSocket = connectToServer();

        // TODO criar thread de leitura de mensagens do servidor
        // TODO implementar sistema de processamento de comandos
    }

    private static void requestServers() throws IOException, ClassNotFoundException {
        DatagramSocket socket = new DatagramSocket();

        DatagramPacket packetToSend = new DatagramPacket(
                new byte[ClientConnectionPayload.MAX_PAYLOAD_BYTES],
                ClientConnectionPayload.MAX_PAYLOAD_BYTES,
                InetAddress.getByName(clientModel.getUdpServerIp()),
                clientModel.getUdpServerPort()
        );

        UdpUtils.sendObject(socket, packetToSend, new ClientConnectionPayload(ClientsPayloadType.REQUEST_SERVERS));

        ClientConnectionPayload serverResponse = UdpUtils.receiveObject(socket);

        clientModel.setServersList(serverResponse.getServersListCollection());

        socket.close();
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
}
