package com.isec.pd22.utils;

import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;

import java.sql.*;
import java.util.Date;

public class DBCommunicationManager {
    private Connection connection;
    private ClientMSG clientMsg;
    private InternalInfo internalInfo;

    public DBCommunicationManager(ClientMSG clientMsg, InternalInfo internalInfo) throws SQLException {
        this.clientMsg = clientMsg;
        this.internalInfo = internalInfo;
        this.connection = DriverManager.getConnection(internalInfo.getUrl_db());
    }

    public void executeRegisterUser(Query query) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(query.getQuery());
        statement.close();
    }

    public void loginUser(ClientMSG msgClient) throws SQLException {
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

    public Query getRegisterUserQuery(ClientMSG msgClient) {
        Query q;
        String query = "INSERT INTO utilizador VALUES (NULL, " + msgClient.getUsername() + "," + msgClient.getNome() + "," + msgClient.getPassword() +", NULL, NULL)";
        Date time = new Date();
        long unixTime = time.getTime()/1000;
        synchronized (internalInfo) {
            q = new Query(internalInfo.getNumDB(), query, unixTime);
        }
        return q;
    }
}
