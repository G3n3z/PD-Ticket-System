package com.isec.pd22.server.threads;

import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.ServerHeartBeat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

import static com.isec.pd22.utils.Constants.MULTICAST_IP;
import static com.isec.pd22.utils.Constants.MULTICAST_PORT;

public class ServerSocketThread extends Thread
{
    private ServerSocket serverSocket;
    private InternalInfo internalInfo;
    private ArrayList<Thread> allClientThreads;


    public ServerSocketThread(ServerSocket serverSocket, InternalInfo internalInfo)
    {
        this.serverSocket = serverSocket;
        this.internalInfo = internalInfo;
        allClientThreads = new ArrayList<>();
        internalInfo.setPortTcp(serverSocket.getLocalPort());
    }

    public ArrayList<Thread> getAllClientThreads() {
        return allClientThreads;
    }

    @Override
    public void run() {

        while(true)
        {
            //flag para sair do ciclo de atender clientes break;
            if(internalInfo.isFinish())
                break;

            try {
                serverSocket.setSoTimeout(5000);
                //cria socket cliente para receber o cliente aceite pelo serversocket
                Socket clientSocket = serverSocket.accept();

                //adicionar socket criado à lista de sockets atendidos
                internalInfo.getAllClientSockets().add(clientSocket);

                //lançar thread de atendendimento de clientes
                AttendClientThread clientThread = new AttendClientThread(clientSocket, internalInfo);
                clientThread.start();

                internalInfo.incrementNumClients();

                allClientThreads.add(clientThread);

                sendHeartbeat();


            }catch (SocketTimeoutException e){
                continue;
            }catch (IOException e){
                //TODO terminar? (por decidir o que fazer)
            }

        }

        waitClientThreads();
    }

    /**
     * Método para esperar as threads dos clientes abertas.
     * Precorre todas as threads no ArrayList das threads dos clientes e espera que acabem
     */
    public void waitClientThreads(){
        ArrayList<Thread> clientThreads = getAllClientThreads();
        for (Thread t : clientThreads) {
            try {
                t.join();
            }catch (InterruptedException e){

            }
            internalInfo.decrementNumClients();
        }
    }

    private void sendHeartbeat() throws IOException {
        //internalinfo.multicast

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        ServerHeartBeat heartBeat = null; //TODO: criar mensagem de heartbeat com informação atualizada para enviar

        oos.writeObject(heartBeat);

        DatagramPacket dp = new DatagramPacket(baos.toByteArray(), baos.size(), InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);

        //internalinfo.multicast.send(dp)
    }

}
