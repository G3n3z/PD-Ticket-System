package com.isec.pd22.server.threads;

import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.InternalInfo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class AttendClientThread extends Thread{
    private Socket clientSocket;
    private InternalInfo internalInfo;

    public AttendClientThread(Socket clientSocket, InternalInfo internalInfo) {
        this.clientSocket = clientSocket;
        this.internalInfo = internalInfo;
    }

    @Override
    public void run() {
        boolean keepGoing = true;
        while (keepGoing) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

                //TODO mudar Object pela class de mensagem
                ClientMSG clientMsg = (ClientMSG) ois.readObject();

                switch (internalInfo.getStatus())
                {
                    case AVAILABLE -> {handleClientRequest(clientMsg, oos);}
                    case UPDATING -> {}//TODO
                    case UNAVAILABLE -> {
                        closeClient(oos);
                        keepGoing = false;}
                }

            } catch (IOException | ClassNotFoundException e) {
                //TODO tratar excecoes lançadas
                System.out.println("[AttendClientThread] failed comunication with client: "+ e.getMessage());
            }
        }
        //TODO enviar ultima msg ao cliente e fechar
    }

    private void handleClientRequest(ClientMSG msgClient, ObjectOutputStream oos) throws IOException {
        Scanner sc = new Scanner(msgClient.getCommand());
        String cmd = sc.nextLine();
        String[] cmdParts = cmd.split(",");
        if (cmd.startsWith("select")){
            //TODO
        }else if (cmd.startsWith("find")) {
            //TODO
        }else if (cmd.startsWith("insert")){
            //TODO
        }else if (cmd.startsWith("update")){
            //TODO
        }else if (cmd.startsWith("delete")){
            //TODO
        }else {
            System.out.println("[AttendClientThread] Unknown Command");
            return;
        }

        //TODO preencher mensagem de retorno com resultado da query
        String msgRetorno = new String();
        ClientMSG ansmsg = new ClientMSG(msgRetorno);
        oos.writeObject(ansmsg);
    }

    private void closeClient(ObjectOutputStream oos) throws IOException {
        ClientMSG ansMsg = new ClientMSG("shutdown");
        ansMsg.setServerList(internalInfo.getHeatBeats());
        oos.writeObject(ansMsg);
    }

}
