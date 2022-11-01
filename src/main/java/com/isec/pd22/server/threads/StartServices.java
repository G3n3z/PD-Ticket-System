package com.isec.pd22.server.threads;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.models.ServerHeartBeat;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.Comparator;
import java.util.Map;
import java.util.Timer;

public class StartServices extends Thread{

    InternalInfo infoServer;
    MulticastSocket socket;

    Connection connection;

    DBVersionManager dbVersionManager;

    Map<HeartBeat, DatagramPacket> heartBeatsPackage;

    ObjectStream os = new ObjectStream();
    public StartServices(InternalInfo infoServer) {
        this.infoServer = infoServer;
        socket = infoServer.getMulticastSocket();
    }

    @Override
    public void run() {
        DatagramPacket packet = null;
        MulticastMSG msg = null;
        dbVersionManager = new DBVersionManager(connection);
        try {
            packet = new DatagramPacket(new byte[6000], 6000);
            socket.setSoTimeout(30);
            receiveMsg(packet);

        }catch (SocketTimeoutException e){
            System.out.println("[START SERVICES] - Timeout socket multicast");

        }
        catch (IOException | ServerException e){
            System.out.println(e);
            if(!infoServer.isFinish())
                System.out.println("Erro ao connectar ao socket multicast");
            synchronized (infoServer){
                infoServer.setFinish(true);
            }
            return;
        }

        try {
            if(infoServer.getHeatBeats().size() == 0){
                notReceiveHearbeat();
            }else{
                //Se o sinal de vida recebido
                receivedHeartBeat();
            }
        }catch (SQLException | ClassNotFoundException | IOException ex){
            System.out.println(ex);
            synchronized (infoServer){
                infoServer.setFinish(true);
            }
            return;
        }
        closeConnection();
        startThreads();
    }

    private void receivedHeartBeat() throws SQLException, IOException, ClassNotFoundException {
        HeartBeat serverWithMaxDBVersion = infoServer.getHeatBeats().stream().min(Comparator.comparingInt(HeartBeat::getNumVersionDB)).get();
        //Se nao tem ligacao à base de dados
        if(!haveConnectionDatabase(infoServer.getUrl_db())) {
            createDatabaseV1();
            updateDB(serverWithMaxDBVersion);
        }
        else {
            if(!verifyLocalDBVersion(serverWithMaxDBVersion)){
                updateDB(serverWithMaxDBVersion);
            }
        }
    }

    private void notReceiveHearbeat() throws SQLException {
        try {
            startFirstServer();
        }catch (ServerException ex){

            throw ex;
        }
    }

    private void receiveMsg(DatagramPacket packet) throws IOException, SocketTimeoutException {
        MulticastMSG msg = null;

        while(true){
            socket.receive(packet);
            msg = os.readObject(packet, MulticastMSG.class);
            connection = getConnectionDatabaseByUrl(infoServer.getUrl_db());
            if(msg.getTypeMsg() != TypeOfMulticastMsg.HEARTBEAT){
                continue;
            }

            HeartBeat heartBeat = (HeartBeat) msg;
            heartBeat.setTimeMsg();
            heartBeatsPackage.put(heartBeat, packet);
            infoServer.addHeartBeat(heartBeat);
        }


    }

    private void startThreads() {
        ServerSocket serverSocket = null;

        try{
        //inicia serversocket thread
            serverSocket = new ServerSocket(0);
        }catch (IOException e){
           System.out.println("Não foi possivel iniciar recursos");
           return;
        }
        System.out.println("Ready to start");

        ServerSocketThread serverSocketThread = new ServerSocketThread(serverSocket, infoServer);
        serverSocketThread.start();

        Timer timer = new Timer(true);
        HeartBeatTask heartBeatTask = new HeartBeatTask(infoServer);
        timer.scheduleAtFixedRate(heartBeatTask, 0, 10000);

        MulticastThread multicastThread = new MulticastThread(infoServer, timer);
        multicastThread.start();
        try {
            serverSocketThread.join();
        } catch (InterruptedException ignored) {
        }finally {
            heartBeatTask.onlyOnceTime = true;
            heartBeatTask.cancel();
            timer.cancel();
            timer.purge();
        }

        System.out.println("A sair da thread");
    }


    /**
     * @param server  servidor escolhido
     * @return true se a nossa base de dados for maior ou igual
     */
    private boolean verifyLocalDBVersion(HeartBeat server) throws SQLException {
        infoServer.setNumDB(dbVersionManager.getLastVersion());
        return (server.getNumVersionDB() <= infoServer.getNumDB());
    }

    private void updateDB(HeartBeat server) throws IOException, ClassNotFoundException, SQLException {

        DBVersionManager dbVersionManager = new DBVersionManager(connection);

        DatagramPacket datagramPacket = heartBeatsPackage.get(server);

        try {
            ServerSocket serverSocket =  new ServerSocket(0);
            serverSocket.setSoTimeout(2000);

            UpdateDB updateDB = new UpdateDB(TypeOfMulticastMsg.UPDATE_DB, infoServer.getNumDB(),serverSocket.getInetAddress().getHostAddress() ,serverSocket.getLocalPort());
            os.writeObject(datagramPacket, updateDB);
            socket.send(datagramPacket);

            Socket socketUpdate = serverSocket.accept();
            socketUpdate.setSoTimeout(2000);
            ObjectOutputStream oos = new ObjectOutputStream(socketUpdate.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socketUpdate.getInputStream());

            while(true){
                Query q = (Query) ois.readObject();
                dbVersionManager.insertQuery(q);
            }
        }catch (EOFException e){
            return;
        }
        catch (IOException | ClassNotFoundException | SQLException e) {
            throw e;
        }
    }

    private void startFirstServer() throws SQLException {
        if(!haveConnectionDatabase(infoServer.getUrl_db())){
            createDatabaseV1();
        }
    }

    private void createDatabaseV1() {
        File f = new File(getPathToDirectory(infoServer.getUrl()));
        int bytesReads = 0;
        if(!f.mkdir()){
           throw new ServerException("Erro a criar a diretoria");
        }

        try {
            FileOutputStream fos = new FileOutputStream( new File(infoServer.getUrl()));
            FileInputStream fis = new FileInputStream(Constants.INITIAL_DB_BASE_URL);
            while (true){
               byte[] bytes = new byte[4000];
               bytesReads = fis.read(bytes);
               if(bytesReads < 0){
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

        connection = getConnectionDatabaseByUrl(infoServer.getUrl_db());
        if(connection == null){
            throw new ServerException("Erro na conexao a db depois de ser criada a copia");
        }

        new DBVersionManager(connection).createTableVersions();

        infoServer.setNumDB(1);

    }

    private String getPathToDirectory(String url) {
        int last = url.lastIndexOf("/");
        return url.substring(0, last);
    }

    private void closeConnection() {
        if (connection != null){
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
        connection = getConnectionDatabaseByUrl(url);
        boolean haveConnection = connection != null;

        if(haveConnection){
            dbVersionManager.setConnection(connection);
            dbVersionManager.checkIfHaveTableVersion();
            infoServer.setNumDB(dbVersionManager.getLastVersion());
            //connection.close();
        }

        return  haveConnection;
    }



}
