package com.isec.pd22.utils;

import com.isec.pd22.exception.ServerException;
import com.isec.pd22.server.models.Query;

import java.sql.*;
import java.util.Date;

public class DBVersionManager {

    Connection connection;

    public DBVersionManager(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public int getLastVersion() throws SQLException {

        Statement stm = connection.createStatement();
        String query = "Select version from versions order by version";
        ResultSet res = stm.executeQuery(query);
        if(res.next()){
            return res.getInt("version");
        }else{
            return 0;
        }

    }

    public void createTableVersions() {
        try {
            Statement stm = connection.createStatement();
            String query = "CREATE TABLE versions(" +
                            "version INTEGER primary key AUTOINCREMENT NOT NULL," +
                            "sql text," +
                            "timestamp numeric)";

            stm.executeUpdate(query);


            addNewVersion(query);

        }catch (SQLException e) {
            throw new ServerException("Erro ao criar a tabela de versoes " + e.getMessage() );
        }
    }


    public void addNewVersion(String sql) throws SQLException {
        String query = "insert into versions (version , sql, timestamp) values(null, ?, ?)";
        PreparedStatement pstm = connection.prepareStatement(query);
        Date time = new Date();
        long unixTime = time.getTime()/1000;
        pstm.setString(1, sql);
        pstm.setLong(2, unixTime);
        pstm.executeUpdate();

    }

    public void insertQuery(Query query) throws SQLException {
        Statement stm = connection.createStatement();
        stm.executeUpdate(query.getQuery());

        String queryVersion = "insert into versions values(?, ?, ?)";

        PreparedStatement pstm = connection.prepareStatement(queryVersion);
        pstm.setInt(1, query.getNumVersion());
        pstm.setString(2, query.getQuery());
        pstm.setLong(3, query.getTimestamp());
        pstm.executeUpdate();
    }

    public void checkIfHaveTableVersion() {

        try {
            getLastVersion();
        } catch (SQLException e) {
            createTableVersions();
        }

    }
}
