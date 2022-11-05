package com.isec.pd22.server.threads;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.server.models.InternalInfo;

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
                Socket clientSocket = serverSocket.accept();

                internalInfo.getAllClientSockets().add(clientSocket);

                AttendClientThread clientThread = new AttendClientThread(clientSocket, internalInfo);
                clientThread.start();

                synchronized (internalInfo) {
                    internalInfo.incrementNumClients();
                }

                allClientThreads.add(clientThread);

                sendHeartbeat();

            }catch (SocketTimeoutException e){
                continue;
            }catch (IOException e){
                //TODO terminar? (por decidir o que fazer)
            }

        }

        waitClientThreads();
        System.out.println("A sair da thread ServerSocket");
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
            synchronized (internalInfo) {
                internalInfo.decrementNumClients();
            }
        }
    }

    private void sendHeartbeat(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            HeartBeat heartBeat;
            synchronized (internalInfo) {
                heartBeat = new HeartBeat(
                        TypeOfMulticastMsg.HEARTBEAT,
                        internalInfo.getIp(),
                        internalInfo.getPortTcp(),
                        internalInfo.getNumClients(),
                        internalInfo.getStatus(),
                        internalInfo.getNumDB(),
                        internalInfo.getPortUdp()
                );
            }
            oos.writeObject(heartBeat);
            DatagramPacket dp = new DatagramPacket(baos.toByteArray(), baos.size(), InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);
            internalInfo.getMulticastSocket().send(dp);
        } catch (IOException e){
            System.out.println("[ServerSocketThread] - error on sending heartbeat");
        }
    }

}
