package com.isec.pd22.utils;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.models.Reserva;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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


    public boolean existsUserByUsernameOrName(String nome, String userName){
        String query = "SELECT * from utilizador where nome = ? or username = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, nome);
            preparedStatement.setString(2, userName);
            ResultSet res = preparedStatement.executeQuery();
            return res.next();
        } catch (SQLException e) {
            return false;
        }
    }


    public boolean canRegistUser(String nome, String userName){
        String query = "SELECT * from utilizador where nome = ? and username = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, nome);
            preparedStatement.setString(2, userName);
            ResultSet res = preparedStatement.executeQuery();
            return !res.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isLogged(String username){
        String query = "SELECT * from utilizador where username = ? and autenticado = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setInt(2, Authenticated.AUTHENTICATED.ordinal());
            ResultSet res = preparedStatement.executeQuery();
            return !res.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public Query authenticate(String userName){
        String sql =   "update utilizador " +
                       "set autenticado = " + Authenticated.AUTHENTICATED + " " +
                       "where username = " + userName;

        return new Query( internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    public Query editUtilizador(String username, String nome, String password){
        String sql = "UPDATE utilizador " +
                     "set username = " + username + ", nome = " + nome + ", password = " + password;

        return new Query(internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    public List<Reserva> consultasReservadasByUser(int idUser, Payment payment){
        List<Reserva> consultas = new ArrayList<>();
        String query = "SELECT * from reserva where id_utilizador = " + idUser;
        if(payment != null){
            query = query + " and pago = " + payment.ordinal();
        }
        try {
            Statement stm = connection.createStatement();

            ResultSet res = stm.executeQuery(query);
            while (res.next()){
                consultas.add(Reserva.mapToEntity(res));
            }

        } catch (SQLException ignored) {
        }
        return consultas;

    }

    public List<Reserva> consultasReservadasAguardamPagamentoByUser(Payment payment){
        List<Reserva> consultas = new ArrayList<>();
        String query = "SELECT * from reserva";
        if(payment != null){
            query = query + " where pago = " + payment.ordinal();
        }
        try {
            Statement stm = connection.createStatement();

            ResultSet res = stm.executeQuery(query);
            while (res.next()){
                consultas.add(Reserva.mapToEntity(res));
            }

        } catch (SQLException ignored) {
        }
        return consultas;
    }

    

}
