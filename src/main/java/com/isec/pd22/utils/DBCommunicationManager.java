package com.isec.pd22.utils;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.ClientMSG;
import com.isec.pd22.server.models.*;

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

    public Espetaculo getEspetaculoLess24HoursById(int id){
        //TODO check this
        String query = "Select * from espetaculo, lugar where espetaculo.id = ? and lugar.espetaculo_id = ? and espetaculo.data_hora >= datetime('now','-24 hours')";
        Espetaculo espetaculo = null;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, id);
            preparedStatement.setInt(2, id);
            ResultSet res = preparedStatement.executeQuery();
            espetaculo = new Espetaculo();
            int i = 0;
            while (res.next()){
                if(i == 0){
                    espetaculo = Espetaculo.mapToEntity(res);
                    i++;
                }
                Lugar lugar = Lugar.mapToEntity(res);
                if(lugar!= null){
                    espetaculo.getLugares().add(lugar);
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return espetaculo;
    }
    public List<Espetaculo> getEspetaculosLess24Hours(){
        String query = "Select * from espetaculo, lugar where espetaculo.data_hora >= datetime('now','-24 hours')";
        List<Espetaculo> espetaculos = new ArrayList<>();
        try {
            Statement statement = connection.createStatement();
            ResultSet res = statement.executeQuery(query);
            Espetaculo espetaculo = new Espetaculo();
            while (res.next()){
                espetaculo = Espetaculo.mapToEntity(res);
                espetaculos.add(espetaculo);
            }

        } catch (SQLException e) {
            return null;
        }
        return espetaculos;
    }

    public boolean haveReservaByIdUserAndIDEspetaculoNotPayed(int idUser, int idEspetaculo){
        String query = "SELECT * from  reserva where reserva.id_utilizador =  ? and reserva.id_espetaculo = ? " +
                "and reserva.pago = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, idUser);
            statement.setInt(2, idEspetaculo);
            statement.setInt(3, Payment.NOT_PAYED.ordinal());
            ResultSet res = statement.executeQuery();
            if(res.next()){
                return true;
            }

        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    public Query deleteReservaNotPayed(int idReserva){
        String query = "delete from reserva_lugar where reserva_lugar.id_reserva = ?; delete from reserva where id = ? and pago = ?; ";

        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public Query submissionOfReserva(List<Integer> idsLugar, int idUser, int idEspetaculo){
        String query = "insert into reserva values(null, " + Constants.dateToString(new Date()) + ", " + Payment.NOT_PAYED.ordinal()
                + ", " + idUser + ", " + idEspetaculo;

        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }
}
