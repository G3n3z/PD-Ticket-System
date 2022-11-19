package com.isec.pd22.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UtilsFunctions {

    public static Connection getConnectionDatabaseByUrl(String url) {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            return null;
        }
        return connection;
    }


    public static String getPathToDirectory(String url) {
        int last = url.lastIndexOf("/");
        return url.substring(0, last);
    }
}
