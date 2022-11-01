package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.payload.Prepare;
import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.tasks.SaveHeartBeatTask;
import com.isec.pd22.server.tasks.UpdateDBTask;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class MulticastThread extends Thread{


    InternalInfo internalInfo;
    Timer timer;

    ObjectStream objectStream;

    DatagramPacket packet;

    MulticastSocket multicastSocket;

    Query query;

    byte [] bytes;

    long unixStartTimeoutTime;

    Map<HeartBeat, DatagramPacket> heartBeatToPackage;

    public MulticastThread(InternalInfo internalInfo, Timer timer) {
        this.internalInfo = internalInfo;
        this.multicastSocket = internalInfo.getMulticastSocket();
        this.timer = timer;
        objectStream = new ObjectStream();
        bytes = new byte[2000];
        packet = new DatagramPacket(bytes, bytes.length);
    }

    @Override
    public void run() {
        while (true){
            if(internalInfo.isFinish()){
                break;
            }
            try {
                multicastSocket.setSoTimeout(10000);
                multicastSocket.receive(packet);
                MulticastMSG msg = objectStream.readObject(packet, MulticastMSG.class);
                if(msg == null){
                    System.out.println("Erro na rececao de mensagem");
                    continue;
                }
                switch (internalInfo.getStatus()){
                    case AVAILABLE -> responseToMsgAvailable(msg, packet);
                    case UPDATING -> {responseToMsgUpdating(msg, packet);}
                    case UNAVAILABLE -> {responseToMsgUnavailable(msg, packet);}
                }

            }catch (SocketTimeoutException e){
                if(internalInfo.getStatus() == Status.UNAVAILABLE){
                    atualizaDB();
                }
                System.out.println(e);
            }
            catch (IOException e) {
                System.out.println(e);
                break;
            }

        }
        System.out.println("A sair da thread multicast");
    }
    private void responseToMsgAvailable(MulticastMSG msg, DatagramPacket packet) {
        System.out.println("[Multicast Available] " + msg.getTypeMsg());
        switch (msg.getTypeMsg()){
            case HEARTBEAT -> {
                HeartBeat heartBeat = (HeartBeat) msg;
                new SaveHeartBeatTask(internalInfo, heartBeat).start();
                // Se a versao recebida é maior que o nosso
                if(heartBeat.getNumVersionDB() > internalInfo.getNumDB()) {
                    heartBeatToPackage = new HashMap<>();
                    heartBeatToPackage.put(heartBeat, packet);
                    unixStartTimeoutTime = new Date().getTime();
                    synchronized (internalInfo){
                        internalInfo.setStatus(Status.UNAVAILABLE);
                        internalInfo.getAllClientSockets().forEach( socket -> {
                            try {
                                socket.getInputStream().close();
                            } catch (IOException ignored) {
                            }
                        });
                    }
                    sendHeartBeat();
                }
            }
            case UPDATE_DB -> {
                UpdateDB msgUpdate = (UpdateDB) msg;
                new UpdateDBTask(internalInfo, msgUpdate).start();
            }
            case PREPARE -> {
                Prepare prepare = (Prepare) msg;
                synchronized (internalInfo){
                    internalInfo.setStatus(Status.UPDATING);
                }
                query = prepare.getQuery();
                Integer i = prepare.getNumVersion();
                objectStream.writeObject(packet, prepare);
                try {
                    multicastSocket.send(packet);
                } catch (IOException e) {
                    System.out.println("Nao consegui enviar a confirmacao do prepare");
                }

            }
        }
    }

    private void responseToMsgUpdating(MulticastMSG msg, DatagramPacket packet) {
        System.out.println("[Multicast Updating] " + msg.getTypeMsg());
        switch (msg.getTypeMsg()){
            case HEARTBEAT -> {
                HeartBeat heartBeat = (HeartBeat) msg;
                new SaveHeartBeatTask(internalInfo, heartBeat).start();}
            case ABORT -> {
                synchronized (internalInfo){
                    internalInfo.setStatus(Status.AVAILABLE);
                }
                internalInfo.notifyAll();
            }
            case COMMIT -> {
                try {
                    DBVersionManager dbVersionManager = new DBVersionManager(internalInfo.getUrl_db());
                    dbVersionManager.insertQuery(query);
                    dbVersionManager.closeConnection();
                    synchronized (internalInfo){
                        internalInfo.setStatus(Status.AVAILABLE);
                    }
                    internalInfo.notifyAll();
                } catch (SQLException e) {
                    //TODO: Ver como resolver
                    System.out.println(e);
                }
            }
        }
    }

    private void responseToMsgUnavailable(MulticastMSG msg, DatagramPacket packet) {
        System.out.println("[Multicast Unavailable] " + msg.getTypeMsg());
        switch (msg.getTypeMsg()){
            case HEARTBEAT -> {
                HeartBeat heartBeat = (HeartBeat) msg;
                new SaveHeartBeatTask(internalInfo, heartBeat).start();
                heartBeatToPackage.put(heartBeat, packet);
            }

        }
        if((new Date().getTime() - unixStartTimeoutTime) >= 10000){
            atualizaDB();
        }

    }


    private void sendHeartBeat() {
        Timer timer1 = new Timer();
        timer1.schedule(new HeartBeatTask(internalInfo, timer1), 0);
    }

    private void atualizaDB() {
        HeartBeat heartBeat;
        synchronized (internalInfo){
            heartBeat = heartBeatToPackage.keySet().stream().filter(heart -> heart.getStatusServer() == Status.AVAILABLE)
                    .min(HeartBeat::compareTo).orElseThrow(() -> new ServerException("Problemas a encontrar o heatbeat"));
        }
        DatagramPacket datagramPacket = heartBeatToPackage.get(heartBeat);
        DBVersionManager dbVersionManager = null;
        try {
            ServerSocket serverSocket = new ServerSocket(0);

            UpdateDB updateDB = new UpdateDB(TypeOfMulticastMsg.UPDATE_DB, internalInfo.getNumDB(), internalInfo.getIp(), serverSocket.getLocalPort());
            objectStream.writeObject(datagramPacket, updateDB);
            multicastSocket.send(datagramPacket);

            serverSocket.setSoTimeout(3000);
            Socket socket = serverSocket.accept();
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            oos.writeUnshared(internalInfo.getNumDB());

            dbVersionManager = new DBVersionManager(internalInfo.getUrl_db());
            while (true) {
                Query query = (Query) ois.readObject();
                dbVersionManager.insertQuery(query);
                internalInfo.setNumDB(dbVersionManager.getLastVersion());
            }

        }catch (EOFException e){

            synchronized (internalInfo){
                internalInfo.setStatus(Status.AVAILABLE);
            }
            sendHeartBeat();

        }
        catch (IOException | ClassNotFoundException | SQLException e) {
            System.out.println("[Atualiza DB] " + e);
        }finally {
            if(dbVersionManager != null)
                dbVersionManager.closeConnection();

        }
    }
}