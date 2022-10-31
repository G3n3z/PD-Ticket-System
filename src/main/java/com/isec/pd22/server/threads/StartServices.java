package com.isec.pd22.server.threads;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.models.ServerHeartBeat;
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
        DatagramPacket packet = null;
        MulticastMSG msg = null;
        try {
            connectMulticastGroup();
            packet = new DatagramPacket(new byte[6000], 6000);
            socket.setSoTimeout(30000);
            while(true){
                socket.receive(packet);
                msg = os.readObject(packet, MulticastMSG.class);
                connection = getConnectionDatabaseByUrl(infoServer.getUrl_db());
                if(msg.getTypeMsg() != TypeOfMulticastMsg.HEARTBEAT){
                    continue;
                }
                //TODO: FIX
                infoServer.getHeatBeats().add(new ServerHeartBeat());
            }

        }catch (SocketTimeoutException e){
            System.out.println("[START SERVICES] - Timeout socket multicast");
            if(infoServer.getHeatBeats().size() == 0){
                try {
                    startFirstServer();
                }catch (ServerException ex){
                    System.out.println(ex.getMessage());
                    synchronized (infoServer){
                        infoServer.setFinish(true);
                    }
                    closeConnection();
                    return;
                }
            }else{
                //Se o sinal de vida recebido tem uma base de dados com versao superior à nossa
                try {
                    if(!haveConnectionDatabase(infoServer.getUrl_db())) {
                        createDatabaseV1();

                        updateDB();
                    }
                    else if(msg != null && !verifyLocalDBVersion(msg)){
                        updateDB();
                    }
                }
                catch (IOException |ClassNotFoundException ex){
                    System.out.println(ex.getMessage());
                }
            }

            startThreads();
        }
        catch (IOException e) {
            System.out.println("Erro ao connectar ao socket multicast");
        }

    }

    private void startThreads() {
       //TODO:
        System.out.println("Ready to start");
    }


    /**
     * @param msg  Mensagem recebida via multicast
     * @return true se a nossa base de dados for maior ou igual
     */
    private boolean verifyLocalDBVersion(MulticastMSG msg) {
        int localVersion = new DBVersionManager(connection).getLastVersion();
        return (msg.getVersionDB() <= localVersion);
    }

    private void updateDB() throws IOException, ClassNotFoundException {
        System.out.println("Ready to update");
        ServerHeartBeat server = infoServer.getHeatBeats().stream().max((s1, s2) -> s1.getNumOfClients() - s2.getNumOfClients()).get();
        try {
            Socket socketUpdate =  new Socket(server.getIp(), server.getPortTcpUpdateDB());
            ObjectOutputStream oos = new ObjectOutputStream(socketUpdate.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socketUpdate.getInputStream());
            Query q = (Query) ois.readObject();
            // TODO: UPDATE DB
        }catch (EOFException e){
            return;
        }
        catch (IOException | ClassNotFoundException e) {
            throw e;
        }


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

        closeConnection();


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
