package com.isec.pd22;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException {
        System.out.println("Hello world!");
        Connection  connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/databases/BaseDB/PD-2022-23-TP.db");

    }
}