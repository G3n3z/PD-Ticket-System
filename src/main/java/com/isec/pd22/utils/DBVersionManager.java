package com.isec.pd22.utils;

import com.isec.pd22.exception.ServerException;

import java.sql.*;
import java.util.Date;

public class DBVersionManager {

    Connection connection;

    public DBVersionManager(Connection connection) {
        this.connection = connection;
    }


    public int getLastVersion(){
        try {
            Statement stm = connection.createStatement();
            String query = "Select numVersion from versions order by numVersion";
            ResultSet res = stm.executeQuery(query);
            if(res.next()){
                return res.getInt("numVersion");
            }
        }catch (SQLException e) {
            return 1;
        }
        return 1;
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

}
