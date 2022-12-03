package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.*;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.tasks.SaveHeartBeatTask;
import com.isec.pd22.server.tasks.UpdateDBTask;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;
import com.isec.pd22.utils.UdpUtils;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class MulticastThread extends Thread{


    InternalInfo internalInfo;
    Timer timer;

    ObjectStream objectStream;

    DatagramPacket packet;

    MulticastSocket multicastSocket;

    Query query;

    byte [] bytes;

    long unixStartTimeoutTime;

    Set<HeartBeat> heartBeatToPackage;
    DBVersionManager dbVersionManager;

    public MulticastThread(InternalInfo internalInfo, Timer timer) {
        this.internalInfo = internalInfo;
        this.multicastSocket = internalInfo.getMulticastSocket();
        this.timer = timer;
        objectStream = new ObjectStream();
        bytes = new byte[20000];
        packet = new DatagramPacket(bytes, bytes.length);
        dbVersionManager = new DBVersionManager(internalInfo.getConnection());
    }

    @Override
    public void run() {
        while (true){
            if(internalInfo.isFinish()){
                break;
            }
            try {
                multicastSocket.setSoTimeout(20000);
                DatagramPacket dp = new DatagramPacket(new byte[20000], 20000);
                multicastSocket.receive(dp);
                ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData(),0, dp.getLength());
                ObjectInputStream ois = new ObjectInputStream(bais);
                //multicastSocket.receive(packet);
                MulticastMSG msg = (MulticastMSG) ois.readObject();
                //MulticastMSG msg = objectStream.readObject(packet, MulticastMSG.class);
                if(msg == null){
                    continue;
                }

                switch (internalInfo.getStatus()){
                    case AVAILABLE -> responseToMsgAvailable(msg, dp);
                    case UPDATING -> {responseToMsgUpdating(msg, dp);}
                    case UNAVAILABLE -> {responseToMsgUnavailable(msg, dp);}
                }

            }catch (SocketTimeoutException e){
                if(internalInfo.getStatus() == Status.UNAVAILABLE){
                    synchronized (internalInfo){
                        internalInfo.setFinish(true);
                    }
                    return;
                }
                System.out.println("[MulticastThread] - socket timeout: "+ e.getMessage());
            }
            catch (IOException | SQLException e) {
                if (!internalInfo.isFinish()) {
                    sendExitMessage(internalInfo, multicastSocket);
                }
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("[MulticastThread] - erro no cast da class: "+ e.getMessage());
                if (!internalInfo.isFinish())
                    sendExitMessage(internalInfo, multicastSocket);
            }catch (ServerException e){
                System.out.println("[MulticastThread - Erro de atualizacao, vamos terminar] - " + e.getMessage());
                sendExitMessage(internalInfo, multicastSocket);
                synchronized (internalInfo){
                    internalInfo.setFinish(true);
                }
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
                    synchronized (internalInfo){
                        System.out.println("Existe servidor com versao maior");
                        System.out.println("Outro servidor " + heartBeat.getIp() + ":" + heartBeat.getPortUdp() + " db: " + heartBeat.getNumVersionDB());
                        System.out.println("Meu servidor " + internalInfo.getIp() + ":" + internalInfo.getPortUdp() + " db: " + internalInfo.getNumDB());
                        internalInfo.setStatus(Status.UNAVAILABLE);
                        internalInfo.getAllClientSockets().forEach( socket -> {
                            try {
                                socket.getInputStream().close();
                            } catch (IOException ignored) {
                            }
                        });
                    }
                    internalInfo.setMyStatus(Status.UNAVAILABLE);
                    sendHeartBeat();
                    updateDB();
                }
            }
            case UPDATE_DB -> {
                UpdateDB msgUpdate = (UpdateDB) msg;
                if (msgUpdate.getIpDest().equals(internalInfo.getIp()) && msgUpdate.getPortUdpDest() == internalInfo.getPortUdp()) {
                    new UpdateDBTask(internalInfo, msgUpdate).start();
                }
            }
            case PREPARE -> {
                responseToPrepare(msg);
            }
            case EXIT -> {
                Exit e = (Exit) msg;
                internalInfo.removeHeatBeat(e.getHeartBeat());
            }
        }
    }

    private void updateDB() {
        Optional<HeartBeat> optional;
        HeartBeat server;
        synchronized (internalInfo.getHeatBeats()){
            optional =  internalInfo.getHeatBeats().stream().filter(heart -> heart.getStatusServer() == Status.AVAILABLE)
                    .min(HeartBeat::compareTo);
        }
        if (optional.isPresent()){
            server = optional.get();
        }else {
            synchronized (internalInfo){
                internalInfo.setStatus(Status.AVAILABLE);
            }
            return;
        }

        System.out.println("Tentativa de atualização 1");
        try {
            UdpUtils.updateDB(server, multicastSocket, internalInfo, dbVersionManager);


        }catch (Exception e){
            System.out.println("Tentativa de atualização 2");
            internalInfo.setConnection(UdpUtils.restartDB(server, multicastSocket, internalInfo, dbVersionManager));
        }
        synchronized (internalInfo){
            internalInfo.setStatus(Status.AVAILABLE);
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
                    System.out.println("[MULTICASTHREAD] - Vou fazer insert query");
                    System.out.println(query.getQuery());
                    try {

                        dbVersionManager.insertQuery(query);
                        synchronized (internalInfo){
                            internalInfo.setNumDB(internalInfo.getNumDB()+1);
                        }
                    }catch (SQLException e) {
                        synchronized (internalInfo){
                            internalInfo.setStatus(Status.UNAVAILABLE);
                            internalInfo.getAllClientSockets().forEach( socket -> {
                                try {
                                    socket.getInputStream().close();
                                } catch (IOException ignored) {
                                }
                            });
                        }
                        internalInfo.setMyStatus(Status.UNAVAILABLE);
                        sendHeartBeat();
                        updateDB();
                        return;
                    }
                }

                internalInfo.notifyAllObservers();
                synchronized (internalInfo.getStatus()){
                    internalInfo.setStatus(Status.AVAILABLE);
                }
                internalInfo.lock.lock();
                internalInfo.condition.signalAll();
                internalInfo.lock.unlock();

            }case EXIT -> {
                Exit e = (Exit) msg;
                internalInfo.removeHeatBeat(e.getHeartBeat());
            }
        }
    }

    private void responseToMsgUnavailable(MulticastMSG msg, DatagramPacket packet) throws SQLException, IOException, ClassNotFoundException {
        System.out.println("[Multicast Unavailable] " + msg.getTypeMsg());
        switch (msg.getTypeMsg()){
            case HEARTBEAT -> {
                HeartBeat heartBeat = (HeartBeat) msg;
                new SaveHeartBeatTask(internalInfo, heartBeat).start();
                heartBeatToPackage.add(heartBeat);
            }case EXIT -> {
                Exit e = (Exit) msg;
                internalInfo.removeHeatBeat(e.getHeartBeat());
            }

        }
        updateDB();
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
//        synchronized (internalInfo){
//            heartBeat = heartBeatToPackage.keySet().stream().filter(heart -> heart.getStatusServer() == Status.AVAILABLE)
//                    .min(HeartBeat::compareTo).orElseThrow(() -> new ServerException("Problemas a encontrar o heatbeat"));
//        }
        DatagramPacket datagramPacket = null;//heartBeatToPackage.get(heartBeat);
        DBVersionManager dbVersionManager = null;
        try {
            ServerSocket serverSocket = new ServerSocket(0);

            UpdateDB updateDB = new UpdateDB(TypeOfMulticastMsg.UPDATE_DB, internalInfo.getNumDB(), internalInfo.getIp(), serverSocket.getLocalPort());
            objectStream.writeObject(datagramPacket, updateDB);
            multicastSocket.send(datagramPacket);

            serverSocket.setSoTimeout(7000);
            Socket socket = serverSocket.accept();
            System.out.println("arualizaDB - aceitou conecao");
            socket.setSoTimeout(10000);
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

    public static void sendExitMessage(InternalInfo internalInfo, MulticastSocket multicastSocket) {
        Exit exit = new Exit(new HeartBeat(internalInfo.getIp(), internalInfo.getPortUdp()));
        try {
            DatagramPacket packet = new DatagramPacket(new byte[20000], 20000, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            new ObjectStream().writeObject(packet, exit);
            multicastSocket.send(packet);
        } catch (IOException e) {
            System.out.println("[MULTICASTHREAD] - Nao foi possivel mandar mensagem exit " + e);
        }
    }
}
