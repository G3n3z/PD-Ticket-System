package com.isec.pd22.utils;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Role;
import com.isec.pd22.enums.Status;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DBVersionManager {

    Connection connection;

    public DBVersionManager(Connection connection) {
        this.connection = connection;
    }

    public DBVersionManager(String url_db) throws SQLException {
        connection = DriverManager.getConnection(url_db);
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public int getLastVersion() throws SQLException {
        ResultSet res;
        Statement stm;
        synchronized (connection) {
            stm = connection.createStatement();
            String query = "Select version from versions order by version desc";
            res = stm.executeQuery(query);

        }
        Integer version;
        if(res.next()){
            version = res.getInt("version");
        }else{
            version = 0;
        }
        closeStatment(stm);
        return version;
    }

    public void createTableVersions() {
        try {
            Statement stm = connection.createStatement();
            String query = "CREATE TABLE versions(" +
                            "version INTEGER primary key AUTOINCREMENT NOT NULL," +
                            "sql text," +
                            "timestamp numeric)";

            stm.executeUpdate(query);
            stm.close();

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
        pstm.close();

    }

    public void insertQuery(Query query) throws SQLException {
        synchronized (connection) {
            Statement stm = connection.createStatement();
            stm.executeUpdate(query.getQuery());

            String queryVersion = "insert into versions values(null, ?, ?)";

            PreparedStatement pstm = connection.prepareStatement(queryVersion);
            pstm.setString(1, query.getQuery());
            pstm.setLong(2, query.getTimestamp());
            pstm.executeUpdate();
            closeStatment(stm);
            closeStatment(pstm);
            
        }
    }

    private void closeStatment(Statement stm) {
        try {
            if (stm != null){
                stm.close();
            }
        }catch (Exception e){}
    }

    public void checkIfHaveTableVersion() {

        try {
            getLastVersion();
        } catch (SQLException e) {
            createTableVersions();
            createAdmin();
        }

    }

    public List<Query> getAllVersionAfter(int numVersion) throws SQLException {
        List<Query> queries = new ArrayList<>();
        String query = "SELECT * from versions where version > ? order by version asc";
        ResultSet res;
        synchronized (connection){
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, numVersion);
            res = preparedStatement.executeQuery();
        }
        while (res.next()){
            queries.add(mapToQuery(res));
        }
        return queries;
    }

    public Query mapToQuery(ResultSet res) throws SQLException {
        Query query = new Query();
        query.setNumVersion(res.getInt("version"));
        query.setQuery(res.getString("sql"));
        query.setTimestamp(res.getLong("timestamp"));
        return query;
    }

    public void closeConnection() {
        try {
            if(connection!= null && !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public void createAdmin() {
        Statement stm = null;
        try {
            stm = connection.createStatement();
            String password = BCrypt.hashpw("admin", BCrypt.gensalt());
            String query = "insert into utilizador values(NULL, 'admin', 'admin', '"+ password + "' ," + Role.ADMIN.ordinal() + ", "+ Authenticated.NOT_AUTHENTICATED.ordinal() + ")";
            stm.executeUpdate(query);
            stm.close();

            addNewVersion(query);

        }catch (SQLException e) {
            throw new ServerException("Erro ao criar a tabela de versoes " + e.getMessage() );
        }finally {
            closeStatment(stm);
        }
    }

    public void deleteDB(InternalInfo internalInfo) {

        synchronized (internalInfo){
            internalInfo.setStatus(Status.UNAVAILABLE);
        }
        System.out.println("Base de dados inconsistente - Vamos eliminar a nossa e pedir novos dados");

    }
}
