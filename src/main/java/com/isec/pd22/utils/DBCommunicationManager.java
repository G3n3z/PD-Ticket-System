package com.isec.pd22.utils;

import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.InternalInfo;

import java.io.ObjectOutputStream;
import java.sql.*;

public class DBCommunicationManager {
    private Connection connection;
    private ClientMSG clientMsg;

    public DBCommunicationManager(ClientMSG clientMsg, InternalInfo internalInfo) throws SQLException {
        this.clientMsg = clientMsg;
        this.connection = DriverManager.getConnection(internalInfo.getUrl_db());
    }

    public void registerUser(ClientMSG msgClient, ObjectOutputStream oos) {
        //TODO
    }

    public void loginUser(ClientMSG msgClient, ObjectOutputStream oos) throws SQLException {
        Statement stm = connection.createStatement();
        String query = "SELECT * FROM utilizador WHERE nome like '%" +
                msgClient.getUsername() + "%' AND password like " + msgClient.getPassword();
        ResultSet result = stm.executeQuery(query);
    }

    public void close() {
        try {
            if(connection != null)
                connection.close();
        } catch (SQLException ignored) {

        }

    }
}
