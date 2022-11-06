package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.*;
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
        bytes = new byte[20000];
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
                System.out.println("MSG RECEBIDA: " + msg.getTypeMsg() + " from " + packet.getPort());
                switch (internalInfo.getStatus()){
                    case AVAILABLE -> responseToMsgAvailable(msg, packet);
                    case UPDATING -> {responseToMsgUpdating(msg, packet);}
                    case UNAVAILABLE -> {responseToMsgUnavailable(msg, packet);}
                }

            }catch (SocketTimeoutException e){
                if(internalInfo.getStatus() == Status.UNAVAILABLE){
                    atualizaDB();
                }
                e.printStackTrace();
            }
            catch (IOException e) {

                e.printStackTrace();
                //                System.out.println(e);
//                break;
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
                responseToPrepare(msg);
            }
        }
    }

    private void responseToMsgUpdating(MulticastMSG msg, DatagramPacket packet) {
        System.out.println("[Multicast Updating] " + msg.getTypeMsg());
        switch (msg.getTypeMsg()){
            case HEARTBEAT -> {
                HeartBeat heartBeat = (HeartBeat) msg;
                new SaveHeartBeatTask(internalInfo, heartBeat).start();}
            case PREPARE -> {
                responseToPrepare(msg);
            }
            case ABORT -> {
                synchronized (internalInfo){
                    internalInfo.setStatus(Status.AVAILABLE);
                }
                internalInfo.lock.lock();
                internalInfo.condition.signalAll();
                internalInfo.lock.unlock();
            }
            case COMMIT -> {
                Commit commit = (Commit) msg;
                if (!(commit.getIp().equalsIgnoreCase(internalInfo.getIp()) && commit.getPortUdp() == internalInfo.getPortUdp())){
                    try {
                        DBVersionManager dbVersionManager = new DBVersionManager(internalInfo.getUrl_db());
                        dbVersionManager.insertQuery(query);
                        dbVersionManager.closeConnection();
                    }catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
                synchronized (internalInfo){
                    internalInfo.setStatus(Status.AVAILABLE);
                }
                internalInfo.lock.lock();
                internalInfo.condition.signalAll();
                internalInfo.lock.unlock();

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

    void responseToPrepare(MulticastMSG msg){
        Prepare prepare = (Prepare) msg;
        synchronized (internalInfo){
            internalInfo.setStatus(Status.UPDATING);
        }
        query = prepare.getQuery();

        try {
            packet = new DatagramPacket(new byte[3000], 3000, InetAddress.getByName(prepare.getIp()), prepare.getConfirmationUDPPort());
            prepare.setIp(internalInfo.getIp());
            prepare.setPortUdpClients(internalInfo.getPortUdp());
            objectStream.writeObject(packet, prepare);
            multicastSocket.send(packet);
        } catch (IOException e) {
            synchronized (internalInfo){
                internalInfo.setStatus(Status.AVAILABLE);
            }
            System.out.println("Nao consegui enviar a confirmacao do prepare");
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
