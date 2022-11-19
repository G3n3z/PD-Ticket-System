package com.isec.pd22.utils;

import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.MulticastMSG;
import com.isec.pd22.payload.ServersRequestPayload;
import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;

public class UdpUtils {

    public static <T> void sendObject(DatagramSocket socket, DatagramPacket packetToSend, T object) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        outputStream.writeObject(object);

        byte[] byteStreamMessage = byteArrayOutputStream.toByteArray();

        packetToSend.setData(byteStreamMessage);
        packetToSend.setLength(byteStreamMessage.length);

        socket.send(packetToSend);
    }

    public static <T> T receiveObject(DatagramSocket socket) throws IOException, ClassNotFoundException {
        DatagramPacket packet = new DatagramPacket(
                new byte[ServersRequestPayload.MAX_PAYLOAD_BYTES],
                ServersRequestPayload.MAX_PAYLOAD_BYTES
        );

        return receiveObject(socket, packet);
    }

    public static <T> T receiveObject(DatagramSocket socket, DatagramPacket packet) throws IOException, ClassNotFoundException {
        socket.receive(packet);

        ByteArrayInputStream byteArrayOutputStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayOutputStream);

        return (T) inputStream.readObject();
    }

    public static void updateDB(HeartBeat server, MulticastSocket socket, InternalInfo internalInfo, DBVersionManager dbVersionManager) throws IOException, ClassNotFoundException, SQLException {

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
            System.out.println("[Utils Update] - " + e);
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
    private static void createDatabaseV1(InternalInfo internalInfo) {
        File f = new File(UtilsFunctions.getPathToDirectory(internalInfo.getUrl()));
        int bytesReads = 0;
//        if (!f.mkdir()) {
//            throw new ServerException("Erro a criar a diretoria");
//        }

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

    public static Connection createInitialData(InternalInfo internalInfo){
        Connection connection = UtilsFunctions.getConnectionDatabaseByUrl(internalInfo.getUrl_db());
        if(connection == null){

            throw new ServerException("Erro na conexao a db depois de ser criada a copia");
        }

        DBVersionManager dbVersionManager = new DBVersionManager(connection);
        dbVersionManager.createTableVersions();
        dbVersionManager.createAdmin();


        internalInfo.setNumDB(2);
        return connection;
    }
    public static Connection restartDB(HeartBeat server, MulticastSocket socket, InternalInfo internalInfo, DBVersionManager dbVersionManager)  {
        createDatabaseV1(internalInfo);
        Connection connection = createInitialData(internalInfo);
        try {
            updateDB(server, socket, internalInfo, dbVersionManager);

        }catch (SQLException | IOException| ClassNotFoundException e){
            throw new ServerException("Tentamos atualizar duas vezes a base de dados e nao conseguimos");
        }

        return connection;
    }

}
