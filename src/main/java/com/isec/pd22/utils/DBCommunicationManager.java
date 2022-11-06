package com.isec.pd22.utils;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class DBCommunicationManager {
    private Connection connection;
    private ClientMSG clientMsg;
    private InternalInfo internalInfo;



    public DBCommunicationManager(ClientMSG clientMsg, InternalInfo internalInfo) throws SQLException {
        this.clientMsg = clientMsg;
        this.internalInfo = internalInfo;
        this.connection = DriverManager.getConnection(internalInfo.getUrl_db());
    }

    public DBCommunicationManager(InternalInfo internalInfo, Connection connection) {
        this.connection = connection;
        this.internalInfo = internalInfo;
    }

    public void executeUserQuery(Query query) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate(query.getQuery());
        statement.close();
    }

    public boolean checkUserLogin(String username, String password) throws SQLException {
        String query = "SELECT * from utilizador where username = ? and password = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet res = preparedStatement.executeQuery();
            return res.next();
        } catch (SQLException e) {
            return false;
        }
    }

    public void close() {
        try {
            if(connection != null)
                connection.close();
        } catch (SQLException ignored) {
        }
    }

    public Query getRegisterUserQuery(String username, String name, String password) {
        Query q;
        String query = "INSERT INTO utilizador VALUES (NULL, '" + username + "', '" + name + "', '" + password +"', " + 1
                +", " + 0 + ")";
        synchronized (internalInfo) {
            q = new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
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
            return !res.next();
        } catch (SQLException e) {
            return true;
        }
    }


    public boolean canEditUser(String nome, String userName){
        String query = "SELECT * from utilizador where nome = ? and username = ?";
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

    public Query setAuthenticate(String userName, Authenticated authenticated){
        String sql =   "update utilizador " +
                       "set autenticado = " + authenticated.ordinal() + " " +
                       "where username = '" + userName + "'";

        return new Query( internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    public Query editUtilizador(String username, String nome, String password){
        String sql = "UPDATE utilizador " +
                     "set username = " + username + ", nome = " + nome + ", password = " + password;

        return new Query(internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    //Será consultaReservasByUser?
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

    //Será consultaReservasByAdmin?
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

    public List<Reserva> getAllReservas(){
        List<Reserva> reservas = new ArrayList<>();
        String query = "SELECT * from reserva order by data_hora desc";
        try {
            Statement stm = connection.createStatement();

            ResultSet res = stm.executeQuery(query);
            while (res.next()){
                reservas.add(Reserva.mapToEntity(res));
            }

        } catch (SQLException ignored) {
        }
        return reservas;
    }

    public Espetaculo getEspetaculoLess24HoursById(int id){
        //TODO check this
        String query = "Select * from espetaculo, lugar where espetaculo.id = ? and lugar.espetaculo_id = ? and espetaculo.data_hora >= datetime('now','+24 hours')";
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

    public Espetaculo getEspetaculoDetailsByIdWithLugares(int idEspetaculo){
        String queryEspec = "Select * from espetaculo where espetaculo.id = ?";
        String query = "Select lugar.id, fila, assento, preco, espetaculo_id from espetaculo, lugar where espetaculo.id = ? and lugar.espetaculo_id = ?";
        String query2 = "SELECT * from reserva_lugar, reserva where reserva_lugar.id_lugar = ? " +
                "and reserva_lugar.id_reserva = reserva.id";
        Espetaculo espetaculo = null;
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(queryEspec);
            preparedStatement.setInt(1, idEspetaculo);
            ResultSet res = preparedStatement.executeQuery();
            if(res.next()){
                espetaculo = Espetaculo.mapToEntity(res);
            }
            preparedStatement.close();

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, idEspetaculo);
            preparedStatement.setInt(2, idEspetaculo);
            res = preparedStatement.executeQuery();

            PreparedStatement preparedStatement2 = connection.prepareStatement(query2);
            Reserva reserva = null;
            while (res.next()){
                Lugar lugar = Lugar.mapToEntity(res);
                if(lugar != null){
                    espetaculo.getLugares().add(lugar);
                    preparedStatement2.setInt(1, lugar.getIdLugar());
                    ResultSet resultSet = preparedStatement2.executeQuery();
                    if(resultSet.next()){
                        reserva = Reserva.mapToEntity(resultSet);
                    }else {
                        reserva = null;
                    }
                    lugar.setReserva(reserva);
                }
            }

        } catch (SQLException e) {
            return null;
        }
        return espetaculo;


    }
    public List<Espetaculo> getEspetaculosAfter24Hours(){
        String query = "Select * from espetaculo where espetaculo.data_hora > strftime('%Y/%m/%d %H:%M', 'now', '+24 hours')";

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

    public Query logout(int idUser){
        String sql = "UPDATE utilizador " +
                     "set autenticado = " + Authenticated.NOT_AUTHENTICATED.ordinal() +
                     " where id = " + idUser;
        return new Query(internalInfo.getNumDB()+1, sql, new Date().getTime());
    }

    public boolean canRemoveEspecatulo(int idEspetaculo){
        String query = "";
        try {
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet res = statement.executeQuery();
            if(res.next()){
                return true;
            }

        } catch (SQLException e) {
            return false;
        }
        return false;


    }

    public User getUser(String username) {
        String query = "SELECT * from utilizador where username = ?";
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            ResultSet res = statement.executeQuery();
            if(res.next()){
               return User.mapToEntity(res);
            }

        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    public Query insertEspetaculo(Espetaculo espetaculo) {
        

        String query = "insert into espetaculo(descricao, tipo, data_hora, duracao, local, localidade, pais, classificacao_etaria) " +
                "values ( '" + espetaculo.getDescricao() + "', '" + espetaculo.getTipo() + "','" + Constants.dateToString(espetaculo.getData_hora()) +
                "', " + espetaculo.getDuracao() + ", '" + espetaculo.getLocal() + "', '" + espetaculo.getLocalidade() + "', '" +
                espetaculo.getPais() + "', '" + espetaculo.getClassificacao_etaria() + "'); ";

       return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public void insertLugares(List<Lugar> lugares, Query query) {
        for (Lugar lugar : lugares) {
            String sql = "insert into lugar values(null, '" + lugar.getFila() + "', '" + lugar.getAssento() + "', " +
                    lugar.getPreco() + ", (Select max(e.id) from espetaculo e)); ";
            query.setQuery(query.getQuery() + sql);
        }
    }
}
