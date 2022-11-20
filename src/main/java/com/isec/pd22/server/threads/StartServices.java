package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;
import com.isec.pd22.utils.UdpUtils;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StartServices extends Thread {

    InternalInfo internalInfo;
    MulticastSocket socket;

    DatagramSocket serversRequestSocket;

    Connection connection;

    DBVersionManager dbVersionManager;

    Timer timer = null;
    HeartBeatTask heartBeatTask = null;

    ServerSocket serverSocket;
    ObjectStream os = new ObjectStream();
    public StartServices(InternalInfo infoServer) {
        this.internalInfo = infoServer;
        socket = infoServer.getMulticastSocket();
    }

    @Override
    public void run() {
        DatagramPacket packet = null;
        MulticastMSG msg = null;

        try {
            try {
                connection = DriverManager.getConnection(internalInfo.getUrl_db());
            }catch (SQLException e){
                System.out.println("Base de dados nao existente");
            }
            dbVersionManager = new DBVersionManager(connection);
            packet = new DatagramPacket(new byte[20000], 20000);
            System.out.println("Aqui" + new Date());
            socket.setSoTimeout(5000);
            receiveMsg(packet);


        }
        catch (IOException | ServerException e){
            System.out.println(e);
            if(!internalInfo.isFinish())
                System.out.println("Erro ao connectar ao socket multicast");
            synchronized (internalInfo){
                internalInfo.setFinish(true);
            }
            return;
        }

        try {

            if(internalInfo.getHeatBeats().size() == 0){

                notReceiveHearbeat();
            } else {
                // Se o sinal de vida recebido
                receivedHeartBeat();
            }
        } catch (SQLException | ClassNotFoundException | IOException ex) {
            System.out.println(ex);

            synchronized (internalInfo){
                internalInfo.setFinish(true);
            }
            closeConnection();
            return;
        }
        startThreads();
    }

    public void close() {
        if (serversRequestSocket != null) {
            serversRequestSocket.close();
        }

        if (socket != null) {
            socket.close();
        }

        internalInfo.closeInputStreams();
    }

    private void receivedHeartBeat() throws SQLException, IOException, ClassNotFoundException {

        HeartBeat serverWithMaxDBVersion = internalInfo.getHeatBeats().stream().min(Comparator.comparingInt(HeartBeat::getNumVersionDB)).get();
        //Se nao tem ligacao à base de dados
        if(!haveConnectionDatabase(internalInfo.getUrl_db())) {

            createDatabaseV1();
            connection = createInitialData(internalInfo);
            dbVersionManager = new DBVersionManager(connection);
            UdpUtils.updateDB(serverWithMaxDBVersion, socket, internalInfo, dbVersionManager);
            //updateDB(serverWithMaxDBVersion);
        } else {
            if (!verifyLocalDBVersion(serverWithMaxDBVersion)) {
                UdpUtils.updateDB(serverWithMaxDBVersion, socket, internalInfo, dbVersionManager);
            }
        }
    }

    private void notReceiveHearbeat() throws SQLException {
        try {
            startFirstServer();
        } catch (ServerException ex) {

            throw ex;
        }
    }

    private void receiveMsg(DatagramPacket packet) throws IOException {
        MulticastMSG msg = null;

        long startTime = new Date().getTime();
        while( (new Date().getTime() - startTime) < Constants.INITIAL_TIMEOUT){
            try {
                socket.receive(packet);
            }catch (SocketTimeoutException e){
                System.out.println("[START SERVICES] - Timeout socket multicast");
                continue;
            }
            msg = os.readObject(packet, MulticastMSG.class);
            connection = getConnectionDatabaseByUrl(internalInfo.getUrl_db());
            if(msg.getTypeMsg() != TypeOfMulticastMsg.HEARTBEAT){
                continue;
            }

            HeartBeat heartBeat = (HeartBeat) msg;
            heartBeat.setTimeMsg();
            internalInfo.addHeartBeat(heartBeat);
        }

    }

    private void startThreads() {
        serverSocket = null;
        startCondition();
        try {
            // inicia serversocket thread
            serverSocket = new ServerSocket(0);
            serversRequestSocket = new DatagramSocket(internalInfo.getPortUdp());
        }
        catch (IOException e) {
            System.out.println("Não foi possivel iniciar recursos");
            return;
        }

        System.out.println("Ready to start");
        internalInfo.setStatus(Status.AVAILABLE);
        internalInfo.setConnection(connection);
        ServerSocketThread serverSocketThread = new ServerSocketThread(serverSocket, internalInfo);
        serverSocketThread.start();

        ServersRequestThread serversRequestThread = new ServersRequestThread(serversRequestSocket, internalInfo);
        serversRequestThread.start();

        timer = new Timer(true);
        heartBeatTask = new HeartBeatTask(internalInfo);
        timer.scheduleAtFixedRate(heartBeatTask, 0, 10000);
        MulticastThread multicastThread = new MulticastThread(internalInfo, timer);
        multicastThread.start();

        try {
            serverSocketThread.join();
            multicastThread.join();
            socket.close();
            serverSocket.close();
        } catch (InterruptedException | IOException ignored) {
        }

        System.out.println("A sair da thread Start Services");
    }

    public void finishTimer(){
        if (heartBeatTask != null){
            heartBeatTask.onlyOnceTime = true;
            heartBeatTask.cancel();
        }
        if (timer != null){
            timer.cancel();
            timer.purge();
        }

    }

    private void startCondition() {
        internalInfo.lock = new ReentrantLock();
        internalInfo.condition = internalInfo.lock.newCondition();
    }

    /**
     * @param server servidor escolhido
     * @return true se a nossa base de dados for maior ou igual
     */
    private boolean verifyLocalDBVersion(HeartBeat server) throws SQLException {
        internalInfo.setNumDB(dbVersionManager.getLastVersion());
        return (server.getNumVersionDB() <= internalInfo.getNumDB());
    }

    private void updateDB(HeartBeat server) throws IOException, ClassNotFoundException, SQLException {

        //DBVersionManager dbVersionManager = new DBVersionManager(connection);
        DatagramPacket datagramPacket = new DatagramPacket(new byte[2000], 2000, InetAddress.getByName(Constants.MULTICAST_IP),
                Constants.MULTICAST_PORT);
        ObjectStream os = new ObjectStream();
        ObjectOutputStream oos = null; ObjectInputStream ois = null;

        try {
            ServerSocket serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(10000);

            UpdateDB updateDB = new UpdateDB(TypeOfMulticastMsg.UPDATE_DB, internalInfo.getNumDB(),serverSocket.getInetAddress().getHostAddress() ,serverSocket.getLocalPort());
            updateDB.setIpDest(server.getIp());
            updateDB.setPortUdpDest(server.getPortUdp());


            os.writeObject(datagramPacket, updateDB);
            socket.send(datagramPacket);

            Socket socketUpdate = serverSocket.accept();
            socketUpdate.setSoTimeout(10000);
            oos = new ObjectOutputStream(socketUpdate.getOutputStream());
            ois = new ObjectInputStream(socketUpdate.getInputStream());

            oos.writeObject(internalInfo.getNumDB());
            while(true){
                Query q = (Query) ois.readObject();
                dbVersionManager.insertQuery(q);
            }
        } catch (EOFException e) {
            return;
        } catch (IOException | ClassNotFoundException | SQLException e) {
            throw e;
        }finally {
            internalInfo.setNumDB(dbVersionManager.getLastVersion());
            if (ois != null){
                ois.close();
            }
            if (oos != null){
                oos.close();
            }
        }
    }

    private void startFirstServer() throws SQLException {

        if(!haveConnectionDatabase(internalInfo.getUrl_db())){
            createDatabaseV1();
            connection = createInitialData(internalInfo);
            dbVersionManager = new DBVersionManager(connection);
        }
    }

    private void createDatabaseV1() {
        File f = new File(getPathToDirectory(internalInfo.getUrl()));
        int bytesReads = 0;
        if (!f.mkdir()) {
            throw new ServerException("Erro a criar a diretoria");
        }

        try {

            FileOutputStream fos = new FileOutputStream( new File(internalInfo.getUrl()));

            FileInputStream fis = new FileInputStream(Constants.INITIAL_DB_BASE_URL);
            while (true) {
                byte[] bytes = new byte[4000];
                bytesReads = fis.read(bytes);
                if (bytesReads < 0) {
                    break;
                }
                fos.write(bytes, 0, bytesReads);
            }
            fos.close();
            fis.close();
        } catch (FileNotFoundException e) {
            throw new ServerException("File not found");
        } catch (IOException e) {
            throw new ServerException("Erro na leitura/Escrita dos ficheiros");
        }


    }

    private Connection createInitialData( InternalInfo internalInfo){
        Connection connection = getConnectionDatabaseByUrl(internalInfo.getUrl_db());
        if(connection == null){

            throw new ServerException("Erro na conexao a db depois de ser criada a copia");
        }

        DBVersionManager dbVersionManager = new DBVersionManager(connection);
        dbVersionManager.createTableVersions();
        dbVersionManager.createAdmin();


        internalInfo.setNumDB(2);
        return connection;
    }

    private String getPathToDirectory(String url) {
        int last = url.lastIndexOf("/");
        return url.substring(0, last);
    }

    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private Connection getConnectionDatabaseByUrl(String url) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            return null;
        }
        return connection;
    }

    private boolean haveConnectionDatabase(String url) throws SQLException {
        //connection = getConnectionDatabaseByUrl(url);
        boolean haveConnection = connection != null;

        if (haveConnection) {
            //dbVersionManager.setConnection(connection);
            dbVersionManager.checkIfHaveTableVersion();

            internalInfo.setNumDB(dbVersionManager.getLastVersion());
            //connection.close();

        }

        return haveConnection;
    }

}
