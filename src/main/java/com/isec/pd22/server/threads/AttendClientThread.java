package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.utils.DBCommunicationManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

public class AttendClientThread extends Thread{
    private Socket clientSocket;
    private InternalInfo internalInfo;
    private DBCommunicationManager dbComm;

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

                ClientMSG clientMsg = (ClientMSG) ois.readObject();

                switch (internalInfo.getStatus())
                {
                    case AVAILABLE -> {handleClientRequest(clientMsg, oos);}
                    case UPDATING -> {internalInfo.wait();}//TODO
                    case UNAVAILABLE -> {
                        closeClient();
                        keepGoing = false;}
                }

            } catch (EOFException e){
                synchronized (internalInfo) {
                    if (internalInfo.getStatus() == Status.UNAVAILABLE) {
                        System.out.println("[AttendClientThread] - server closed client connection: " + e.getMessage());
                        closeClient();
                    }
                }
                keepGoing = false;
            } catch (IOException | ClassNotFoundException e) {
                //TODO tratar excecoes lançadas
                System.out.println("[AttendClientThread] - failed comunication with client: "+ e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("[AttendClientThread] - server finished updating: " + e.getMessage());
            }
        }
        //TODO enviar ultima msg ao cliente e fechar
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - error on closing client socket: "+ e.getMessage());
        }
    }

    private void handleClientRequest(ClientMSG msgClient, ObjectOutputStream oos){
        try {
            dbComm = new DBCommunicationManager(msgClient, internalInfo);
            switch (msgClient.getAction()) {
                case REGISTER_USER -> { dbComm.registerUser(msgClient, oos); }
                case LOGIN -> { dbComm.loginUser(msgClient, oos); }
                case EDIT_USER -> {
                }
                case CONSULT_RESERVATION -> {
                }
                case CONSULT_SPECTACLE -> {
                }
                case CHOOSE_SPECTACLE -> {
                }
                case VIEW_SEATS -> {
                }
                case SUBMIT_RESERVATION -> {
                }
                case CANCEL_RESERVATION -> {
                }
                case ADD_SPECTACLE -> {
                }
                case LOGOUT -> {
                }
                case SHUTDOWN -> {
                }

            }
            dbComm.close();
        } catch (SQLException e) {
            System.out.println("[AttendClientThread] - failed to initialize DB communication: "+ e.getMessage());
        }

        //TODO preencher mensagem de retorno com resultado da query e enviar a cliente

    }

    private void closeClient() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ClientMSG ansMsg = new ClientMSG();
            ansMsg.setServerList(internalInfo.getHeatBeats());
            oos.writeObject(ansMsg);
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - failed to send server list" + e.getMessage());
        }
    }

}
