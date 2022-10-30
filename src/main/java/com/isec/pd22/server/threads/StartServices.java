package com.isec.pd22.server.threads;

import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class StartServices extends Thread{

    InternalInfo infoServer;
    MulticastSocket socket;

    Connection connection;

    ObjectStream os = new ObjectStream();
    public StartServices(InternalInfo infoServer) {
        this.infoServer = infoServer;
    }

    @Override
    public void run() {
        try {
            connectMulticastGroup();
            DatagramPacket packet = new DatagramPacket(new byte[6000], 6000);
            socket.setSoTimeout(30);
            socket.receive(packet);
            MulticastMSG msg = os.readObject(packet, MulticastMSG.class);
            connection = getConnectionDatabaseByUrl(infoServer.getUrl_db());

            //Se o sinal de vida recebido tem uma base de dados com versao superior à nossa
            if(!verifyLocalDBVersion(msg)){
               updateDB(packet);
            }
            startThreads();
        }catch (SocketTimeoutException e){
            System.out.println("[START SERVICES] - Timeout socket multicast");
            try {
                startFirstServer();
            }catch (ServerException ex){
                System.out.println(ex.getMessage());
                synchronized (infoServer){
                    infoServer.setFinish(true);
                }
                //TODO: close connection
            }
        }
        catch (IOException e) {
            System.out.println("Erro ao connectar ao socket multicast");
        }

    }

    private void startThreads() {
    }
       //TODO:
    /**
     * @param msg  Mensagem recebida via multicast
     * @return true se a nossa base de dados for maior ou igual
     */
    private boolean verifyLocalDBVersion(MulticastMSG msg) {
        int localVersion = new DBVersionManager(connection).getLastVersion();
        return (msg.getVersionDB() <= localVersion);
    }

    private void updateDB(DatagramPacket packet) {
        //TODO:
    }

    private void startFirstServer() {
        if(haveConnectionDatabase(infoServer.getUrl_db())){
            startThreads();
        }
        else {
            createDatabaseV1();
        }
    }

    private void createDatabaseV1() {
        File f = new File(Constants.BASE_URL_DB +"server_db_"+Integer.toString(infoServer.getPortUdp())
                            +"/"+Constants.NAME_DEFAULT_DB);
        int bytesReads = 0;
        if(!f.mkdirs()){
           throw new ServerException("Erro a criar a diretoria");
        }
        try {
            FileOutputStream fos = new FileOutputStream(f);
            FileInputStream fis = new FileInputStream(Constants.INITIAL_DB_BASE_URL);
            while (bytesReads > -1){
               byte[] bytes = new byte[4000];
               bytesReads = fis.read(bytes);
               fos.write(bytes, 0, bytesReads);
            }

        } catch (FileNotFoundException e) {
            throw new ServerException("File not found");
        } catch (IOException e) {
            throw new ServerException("Erro na leitura/Escrita dos ficheiros");
        }


    }

    private Connection getConnectionDatabaseByUrl(String url) {
        try {
            return DriverManager.getConnection(url);
        } catch (SQLException e) {
            return null;
        }
    }


    private boolean haveConnectionDatabase(String url){
        Connection connection = getConnectionDatabaseByUrl(url);
        boolean haveConnection = connection != null;
        try {
            if(haveConnection)
                connection.close();
        } catch (SQLException ignored) {}

        return  haveConnection;
    }


    private void connectMulticastGroup() throws IOException {

        socket = new MulticastSocket(Constants.MULTICAST_PORT);
        InetAddress group = InetAddress.getByName(Constants.MULTICAST_IP);
        SocketAddress sa = new InetSocketAddress(group, Constants.MULTICAST_PORT);
        NetworkInterface nif = NetworkInterface.getByName("en0");
        socket.joinGroup(sa, nif);

    }
}
