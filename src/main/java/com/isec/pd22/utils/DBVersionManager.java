package com.isec.pd22.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
            return -1;
        }
        return -1;
    }
}
