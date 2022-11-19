package com.isec.pd22.server.tasks;

import com.isec.pd22.payload.UpdateDB;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.utils.DBVersionManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UpdateDBTask extends Thread{

    InternalInfo internalInfo;

    UpdateDB updateDB;

    Connection connection;
    public UpdateDBTask(InternalInfo internalInfo, UpdateDB updateDB) {
        this.internalInfo = internalInfo;
        this.updateDB = updateDB;
        connection = internalInfo.getConnection();
    }

    @Override
    public void run() {
        System.out.println("[UPDATEDBTASK] - Começou o Update" );
        try{
            Socket socket = new Socket(updateDB.getIp(), updateDB.getPortTCP());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            socket.setSoTimeout(10000);
            int numVersion = (int)ois.readObject();
            DBVersionManager dbVersionManager = new DBVersionManager(connection);
            List<Query> queries = dbVersionManager.getAllVersionAfter(numVersion);

            for (Query query : queries) {
                oos.writeUnshared(query);
            }
            ois.close();
            oos.close();
        }catch (EOFException e){
            System.out.println("[UPDATEDBTASK] - Alguem fechou o socket");
        }
        catch (IOException e){
            System.out.println("[UPDATEDBTASK] - Não foi possivel abrir o socket");
        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("[UPDATEDBTASK] - Não foi possivel fornecer os pedidos");
        }
        //System.out.println("[UPDATEDBTASK] - Terminou o Update" );
    }
}
