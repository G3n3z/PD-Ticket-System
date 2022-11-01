package com.isec.pd22.server.threads;

import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.ServerHeartBeat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class ServerSocketThread extends Thread
{
    private ServerSocket serverSocket;
    private InternalInfo internalInfo;
    private ArrayList<Socket> allClientSockets;
    private ArrayList<Thread> allClientThreads;


    public ServerSocketThread(InternalInfo internalInfo)
    {
        this.internalInfo = internalInfo;
        allClientSockets = new ArrayList<>();
        allClientThreads = new ArrayList<>();
    }

    public ArrayList<Socket> getAllClientSockets() { return allClientSockets; }

    public ArrayList<Thread> getAllClientThreads() {
        return allClientThreads;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(0);
            //colocar na internalInfo informação sobre o porto TCP automaticamente atribuído
            internalInfo.setPortTcp(serverSocket.getLocalPort());

        } catch (IOException e) {
            //TODO: ver como tratar

        }
        while(true)
        {
            //flag para sair do ciclo de atender clientes break;
            if(internalInfo.isFinish())
                break;

            try {
                //cria socket cliente para receber o cliente aceite pelo serversocket
                Socket clientSocket = serverSocket.accept();
                //TODO colocar timeout para obrigar a testar a condição da flag??
                clientSocket.setSoTimeout(5000);

                //adicionar socket criado à lista de sockets atendidos
                allClientSockets.add(clientSocket);

                //lançar thread de atendendimento de clientes
                AttendClientThread clientThread = new AttendClientThread(clientSocket, internalInfo);
                clientThread.start();

                internalInfo.incrementNumClients(); //TODO devera ficar assim?? tornar InternalInfo em objeto synchronized?

                allClientThreads.add(clientThread);

                //TODO enviar sinal de vida. Info necessária e método
                ServerHeartBeat heartBeat = new ServerHeartBeat();

            }catch (SocketTimeoutException e){
                continue;
            }catch (IOException e){
                //TODO terminar? (por decidir o que fazer)
            }

        }

        //TODO join das threads dos clientes não pode ser feita aqui. terá de ser feita na camada acima.
        waitClientThreads();
        //TODO ver melhor maneira de fechar os sockets dos clientes
        closeClientSockets();
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

    private void closeClientSockets(){
        ArrayList<Socket> clientSockets = getAllClientSockets();
        for(Socket s : clientSockets){
            try {
                s.close();
            }catch (IOException e) {

            }
        }
    }
}
