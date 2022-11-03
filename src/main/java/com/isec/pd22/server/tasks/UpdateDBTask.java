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
import java.sql.SQLException;
import java.util.List;

public class UpdateDBTask extends Thread{

    InternalInfo internalInfo;

    UpdateDB updateDB;

    public UpdateDBTask(InternalInfo internalInfo, UpdateDB updateDB) {
        this.internalInfo = internalInfo;
        this.updateDB = updateDB;
    }

    @Override
    public void run() {
        //System.out.println("[UPDATEDBTASK] - Começou o Update" );
        try{
            Socket socket = new Socket(updateDB.getIp(), updateDB.getPortTCP());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            socket.setSoTimeout(2000);
            int numVersion = (int)ois.readObject();
            DBVersionManager dbVersionManager = new DBVersionManager(internalInfo.getUrl_db());
            List<Query> queries = dbVersionManager.getAllVersionAfter(numVersion);
            dbVersionManager.closeConnection();
            for (Query query : queries) {
                oos.writeUnshared(query);
            }

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
