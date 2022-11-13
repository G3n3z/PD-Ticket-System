package com.isec.pd22.utils;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.ListPlaces;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DBCommunicationManager {
    private final Connection connection;
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
        PreparedStatement preparedStatement = null;
        synchronized (connection){
            try {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);
                ResultSet res = preparedStatement.executeQuery();
                return res.next();
            } catch (SQLException e) {
                return false;
            }finally {
                closeStatement(preparedStatement);
            }
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
        String query = "INSERT INTO utilizador VALUES (NULL, '" + username + "', '" + name + "', '" + password +"', " +
                Authenticated.NOT_AUTHENTICATED.ordinal()
                +", " + 0 + ")";
        synchronized (internalInfo) {
            q = new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
        }
        return q;
    }


    public boolean existsUserByUsernameOrName(String nome, String userName){
        String query = "SELECT * from utilizador where nome = ? or username = ?";
        PreparedStatement preparedStatement = null;
        synchronized (connection) {
            try {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, nome);
                preparedStatement.setString(2, userName);
                ResultSet res = preparedStatement.executeQuery();
                return !res.next();
            } catch (SQLException e) {
                return true;
            }finally {
                closeStatement(preparedStatement);
            }
        }
    }


    public boolean canEditUser(String nome, String userName){
        String query = "SELECT * from utilizador where nome = ? and username = ?";
        PreparedStatement preparedStatement = null;
        synchronized (connection) {
            try {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, nome);
                preparedStatement.setString(2, userName);
                ResultSet res = preparedStatement.executeQuery();
                return res.next();
            } catch (SQLException e) {
                return false;
            }
        }
    }

    public boolean isLogged(String username){
        String query = "SELECT * from utilizador where username = ? and autenticado = ?";
        PreparedStatement preparedStatement = null;
        synchronized (connection){
            try {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, username);
                preparedStatement.setInt(2, Authenticated.AUTHENTICATED.ordinal());
                ResultSet res = preparedStatement.executeQuery();
                return !res.next();
            } catch (SQLException e) {
                return false;
            }finally {
                closeStatement(preparedStatement);
            }
        }
    }

    public Query setAuthenticate(String userName, Authenticated authenticated){
        String sql =   "update utilizador " +
                       "set autenticado = " + authenticated.ordinal() + " " +
                       "where username = '" + userName + "'";

        return new Query( internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    public Query editUtilizador(int id, String username, String nome, String password){
        String sql = "UPDATE utilizador " +
                     "SET username= '" + username + "', nome= '" + nome;
        if(password != null) {
            sql += "', password= '" + password;
        }
        sql += "' WHERE id= " + id;

        return new Query(internalInfo.getNumDB()+1,sql, new Date().getTime());
    }

    //Será consultaReservasByUser?
    public List<Reserva> consultasReservadasByUser(int idUser, Payment payment){
        List<Reserva> consultas = new ArrayList<>();
        String query = "SELECT * from reserva where id_utilizador = " + idUser;
        if(payment != null){
            query = query + " and pago = " + payment.ordinal();
        }
        Statement stm = null;
        synchronized (connection) {
            try {
                stm = connection.createStatement();

                ResultSet res = stm.executeQuery(query);
                while (res.next()) {
                    consultas.add(Reserva.mapToEntity(res));
                }

            } catch (SQLException ignored) {
            }finally {
                closeStatement(stm);
            }
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
        Statement stm = null;
        synchronized (connection) {
            try {
                stm = connection.createStatement();

                ResultSet res = stm.executeQuery(query);
                while (res.next()) {
                    consultas.add(Reserva.mapToEntity(res));
                }

            } catch (SQLException ignored) {
            }finally {
                closeStatement(stm);
            }
        }
        return consultas;
    }

    public List<Reserva> getAllReservas(){
        List<Reserva> reservas = new ArrayList<>();
        String query = "SELECT * from reserva order by data_hora desc";
        Statement stm = null;
        synchronized (connection) {
            try {
                 stm = connection.createStatement();

                ResultSet res = stm.executeQuery(query);
                while (res.next()) {
                    reservas.add(Reserva.mapToEntity(res));
                }

            } catch (SQLException ignored) {
            }finally {
                closeStatement(stm);
            }
        }
        return reservas;
    }

    public Espetaculo getEspetaculoLess24HoursById(int id){
        //TODO check this
        String query = "Select * from espetaculo, lugar where espetaculo.id = ? and lugar.espetaculo_id = ? and espetaculo.data_hora >= datetime('now','+24 hours')";
        Espetaculo espetaculo = null;
        ResultSet res;
        PreparedStatement preparedStatement = null;
        try {
            synchronized (connection) {
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, id);
                preparedStatement.setInt(2, id);
                res = preparedStatement.executeQuery();
            }
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
        }finally {
            closeStatement(preparedStatement);
        }
        return espetaculo;
    }

    public Espetaculo getEspetaculoDetailsByIdWithLugares(int idEspetaculo){
        String queryEspec = "Select * from espetaculo where espetaculo.id = ?";
        String query = "Select lugar.id, fila, assento, preco, espetaculo_id from espetaculo, lugar where espetaculo.id = ? and lugar.espetaculo_id = ?";
        String query2 = "SELECT * from reserva_lugar, reserva where reserva_lugar.id_lugar = ? " +
                "and reserva_lugar.id_reserva = reserva.id";
        Espetaculo espetaculo = null;
        PreparedStatement preparedStatement2 = null;
        PreparedStatement preparedStatement = null;
        synchronized (connection) {
            try {
                preparedStatement = connection.prepareStatement(queryEspec);
                preparedStatement.setInt(1, idEspetaculo);
                ResultSet res = preparedStatement.executeQuery();
                if (res.next()) {
                    espetaculo = Espetaculo.mapToEntity(res);
                }
                preparedStatement.close();

                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, idEspetaculo);
                preparedStatement.setInt(2, idEspetaculo);
                res = preparedStatement.executeQuery();

                preparedStatement2 = connection.prepareStatement(query2);
                Reserva reserva = null;
                while (res.next()) {
                    Lugar lugar = Lugar.mapToEntity(res);
                    if (lugar != null) {
                        espetaculo.getLugares().add(lugar);
                        preparedStatement2.setInt(1, lugar.getIdLugar());
                        ResultSet resultSet = preparedStatement2.executeQuery();
                        if (resultSet.next()) {
                            reserva = Reserva.mapToEntity(resultSet);
                        } else {
                            reserva = null;
                        }
                        lugar.setReserva(reserva);
                    }
                }

            } catch (SQLException e) {
                return null;
            }finally {
                closeStatement(preparedStatement);
                closeStatement(preparedStatement2);
            }
        }
        return espetaculo;


    }
    public List<Espetaculo> getEspetaculosAfter24Hours(){
        String query = "Select * from espetaculo where espetaculo.data_hora > strftime('%Y/%m/%d %H:%M', 'now', '+24 hours')";
        ResultSet res;
        List<Espetaculo> espetaculos = new ArrayList<>();
        Statement statement = null;
        try {
            synchronized (connection) {
                statement = connection.createStatement();
                res = statement.executeQuery(query);
            }
            Espetaculo espetaculo = new Espetaculo();
            while (res.next()){
                espetaculo = Espetaculo.mapToEntity(res);
                espetaculos.add(espetaculo);
            }


        } catch (SQLException e) {
            return null;
        }finally {
            closeStatement(statement);
        }
        return espetaculos;
    }

    public boolean haveReservaByIdUserAndIDEspetaculoNotPayed(int idUser, int idEspetaculo){
        String query = "SELECT * from  reserva where reserva.id_utilizador =  ? and reserva.id_espetaculo = ? " +
                "and reserva.pago = ?";
        ResultSet res;
        PreparedStatement statement = null;
        try {
            synchronized (connection) {
                statement = connection.prepareStatement(query);
                statement.setInt(1, idUser);
                statement.setInt(2, idEspetaculo);
                statement.setInt(3, Payment.NOT_PAYED.ordinal());
                res = statement.executeQuery();
            }
            if(res.next()){
                return true;
            }

        } catch (SQLException e) {
            return false;
        }finally {
            closeStatement(statement);
        }
        return false;
    }

    public Query deleteReservaNotPayed(int idReserva){
        String query = "delete from reserva_lugar where reserva_lugar.id_reserva= "+ idReserva +
                "; delete from reserva where id= " + idReserva + " and pago= " + 0 + ";";
        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public Query submissionOfReserva(List<Integer> idsLugar, int idUser, int idEspetaculo){
        String query = "insert into reserva values(null, " + Constants.dateToString(new Date()) + ", " + Payment.NOT_PAYED.ordinal()
                + ", " + idUser + ", " + idEspetaculo;

        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public boolean canRemoveEspecatulo(int idEspetaculo){
        String query = "SELECT * FROM reserva WHERE id_espetaculo = " + idEspetaculo + " AND pago = " + 1;
        ResultSet res;
        PreparedStatement statement = null;
        try {
            synchronized (connection) {
                statement = connection.prepareStatement(query);
                res = statement.executeQuery();
            }
            if(res.next())
                return false;

        } catch (SQLException e) {
            return true;
        }finally {
            closeStatement(statement);
        }
        return true;
    }

    public User getUser(String username) {
        String query = "SELECT * from utilizador where username = ?";
        ResultSet res;
        PreparedStatement statement = null;
        try {
            synchronized (connection) {
                statement = connection.prepareStatement(query);
                statement.setString(1, username);
                res = statement.executeQuery();
            }
            if(res.next()){
               return User.mapToEntity(res);
            }

        } catch (SQLException e) {
            return null;
        }finally {
            closeStatement(statement);
        }
        return null;
    }

    private void closeStatement(Statement statement) {
        if (statement != null){
            try {
                statement.close();
            } catch (SQLException ignored) {}
        }
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

    public List<Espetaculo> getEspetaculoWithFilters(Espetaculos espetaculos){
        Map<String,String> filtros = espetaculos.getFiltros();
        String query = "Select * from espetaculo"; ResultSet res;
        boolean first = true;
        if(!filtros.isEmpty()){
            for(Map.Entry<String, String> entry : filtros.entrySet()){
                if(first){
                    query += " where " + entry.getKey() + "= " + entry.getValue();
                    first = false;
                }else{
                    query += " and " + entry.getKey() + "= " + entry.getValue();
                }
            }
        }
        List<Espetaculo> list = new ArrayList<>();
        Statement statement = null;
        try {
            synchronized (connection) {
                statement = connection.createStatement();
                res = statement.executeQuery(query);
            }
            Espetaculo espetaculo = new Espetaculo();
            while (res.next()){
                espetaculo = Espetaculo.mapToEntity(res);
                list.add(espetaculo);
            }

        } catch (SQLException e) {
            System.out.println("[DBCOM] - getEspetaculoWithFilters " + e);
            return new ArrayList<>();
        }finally {
            closeStatement(statement);
        }
        return list;
    }

    public Query deleteSpectacle(int idEspetaculo) {
        String q1 = "DELETE FROM reserva_lugar WHERE id_reserva IN (SELECT reserva.id FROM reserva WHERE id_espetaculo= '" + idEspetaculo + "')";
        String q2 = "DELETE FROM reserva WHERE id_espetaculo= '" + idEspetaculo;
        String q3 = "DELETE FROM lugar WHERE espetaculo_id= '" + idEspetaculo;
        String q4 = "DELETE FROM espetaculo WHERE espetaculo.id= '" + idEspetaculo;
        String query = q1 + "; " + q2 + "'; " + q3 + "'; " + q4 + "'; ";
        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public boolean canCancelReservation(int idReserva) {
        String query = "SELECT * FROM reserva WHERE reserva.id= ? AND pago= ?"; ResultSet res;
        PreparedStatement statement = null;
        try {
            synchronized (connection) {
                statement = connection.prepareStatement(query);
                statement.setInt(1, idReserva);
                statement.setInt(2, 1);
                res = statement.executeQuery(query);
            }
            if (res.next()){
                return false;
            }

        } catch (SQLException e) {
            return true;
        }finally {
            closeStatement(statement);
        }
        return true;
    }

    public boolean canSubmitReservations(ListPlaces list) {
        ResultSet res;
        PreparedStatement statement = null;
        String query = "SELECT * FROM reserva_lugar WHERE id_reserva IN " +
                "(SELECT reserva.id FROM reserva WHERE id_espetaculo= ?) " +
                "AND id_lugar= ?";
            for(Lugar place : list.getPlaces()){
                try {
                    synchronized (connection) {
                        statement = connection.prepareStatement(query);
                        statement.setInt(1, place.getEspetaculo_id());
                        statement.setInt(2, place.getIdLugar());
                        res = statement.executeQuery(query);
                    }
                    if (res.next()){
                        return false;
                    }
                } catch (SQLException ignored) {
                }finally {
                    closeStatement(statement);
                }
            }
        return true;
    }

    public Query submitReservations(ListPlaces list) {
        Lugar dummy = list.getPlaces().get(0);
        String query="INSERT INTO reserva(id, data_hora, pago, id_utilizador, id_espetaculo) " +
                "values(NULL, '" + Constants.dateToString(dummy.getReserva().getData_hora()) + "', '" + Payment.NOT_PAYED.ordinal() +
                "', '" + dummy.getReserva().getIdUser() +
                "', '" + dummy.getReserva().getIdEspectaculo() + "'); ";
        String q2;
        for(Lugar place: list.getPlaces()) {
            q2 = "INSERT INTO reserva_lugar(id_reserva, id_lugar) values((Select max(r.id) from reserva r), '" + place.getIdLugar() + "'); ";
            query += q2;
        }
        return new Query(internalInfo.getNumDB()+1, query, new Date().getTime());
    }

    public int getLastId(String tableName) {
        String query = "SELECT max(id) FROM " + tableName; ResultSet result;
        Statement statement = null;
        int id = -1;
        try {
            synchronized (connection) {
                statement = connection.createStatement();
                result = statement.executeQuery(query);
            }
            if (result.next()){
                id = result.getInt(1);
            }
        } catch (SQLException e) {
            return id;
        }finally {
            closeStatement(statement);
        }
        return id;
    }

    public Query payReservations(RequestListReservas reservas) {
        StringBuffer sb = new StringBuffer();
        List<Integer> list;
        list = reservas.getReservas().stream().map(Reserva::getIdReserva).distinct().toList();

        sb.append("Update reserva " +
                "set pago = ");
        sb.append(Payment.PAYED.ordinal());
        sb.append(" where id in (");

        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if(i < list.size()-1){
                sb.append(",");
            }
        }
        sb.append(");");
        return  new Query(internalInfo.getNumDB()+1, sb.toString(), new Date().getTime());
    }
}
