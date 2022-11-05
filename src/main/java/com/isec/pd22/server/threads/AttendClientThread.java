package com.isec.pd22.server.threads;

import com.isec.pd22.enums.*;
import com.isec.pd22.payload.*;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Register;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.server.models.Reserva;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBCommunicationManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttendClientThread extends Thread{
    private Socket clientSocket;
    private InternalInfo internalInfo;
    private DBCommunicationManager dbComm;
    private Role role;

    public AttendClientThread(Socket clientSocket, InternalInfo internalInfo) {
        this.clientSocket = clientSocket;
        this.internalInfo = internalInfo;
    }

    @Override
    public void run() {
        boolean keepGoing = true;
        while (keepGoing) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

                ClientMSG clientMsg = (ClientMSG) ois.readObject();

                switch (internalInfo.getStatus())
                {
                    case AVAILABLE -> {handleClientRequest(clientMsg, oos);}
                    case UPDATING -> {
                        //TODO enviar mensagem de update para o cliente
                        internalInfo.wait();
                    }
                    case UNAVAILABLE -> {
                        closeClient();
                        keepGoing = false;}
                }

            } catch (EOFException e){
                synchronized (internalInfo) {
                    if (internalInfo.getStatus() == Status.UNAVAILABLE) {
                        System.out.println("[AttendClientThread] - server closed client connection: " + e.getMessage());
                        closeClient();
                    }
                }
                keepGoing = false;
            } catch (IOException | ClassNotFoundException e) {
                //TODO tratar excecoes lançadas
                System.out.println("[AttendClientThread] - failed comunication with client: "+ e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("[AttendClientThread] - server finished updating: " + e.getMessage());
            }
        }
        //TODO enviar ultima msg ao cliente e fechar
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - error on closing client socket: "+ e.getMessage());
        }
    }

    private void handleClientRequest(ClientMSG msgClient, ObjectOutputStream oos){
        try {
            dbComm = new DBCommunicationManager(msgClient, internalInfo);
            ClientMSG ansMsg = new ClientMSG();

            switch (msgClient.getAction()){
                case REGISTER_USER -> {
                    doRegister(msgClient, dbComm, oos);
                }
                case LOGIN -> {
                    doLogin(msgClient, dbComm);

                }
                default -> actionsLogged(msgClient, oos, dbComm);
            }

        } catch (SQLException | IOException e) {
            System.out.println("[AttendClientThread] - failed to initialize DB communication: "+ e.getMessage());
        }

        //TODO preencher mensagem de retorno com resultado da query e enviar a cliente

    }



    private void actionsLogged(ClientMSG msgClient, ObjectOutputStream oos, DBCommunicationManager dbComm) {
        try {
            if (dbComm.isLogged(msgClient.getUser().getUsername())) {
                switch (msgClient.getAction()) {
                    case EDIT_USER -> {
                        if (dbComm.canEditUser(msgClient.getUser().getNome(), msgClient.getUser().getUsername())) {
                            Query query = dbComm.editUtilizador(
                                    msgClient.getUser().getUsername(),
                                    msgClient.getUser().getNome(),
                                    msgClient.getUser().getPassword()
                            );
                            if (startUpdateRoutine(query, internalInfo)) {
                                dbComm.executeUserQuery(query);
                            } else {
                                //TODO enviar mensagem a cliente: impossel realizar transação, tente mais tarde
                            }
                        }
                    }
                    case CONSULT_UNPAYED_RESERVATION -> {
                        List<Reserva> unpayedReservations; //TODO Lista para colocar na estrutura de comunicação
                        if (role == Role.ADMIN) {
                            unpayedReservations = dbComm.consultasReservadasAguardamPagamentoByUser(Payment.NOT_PAYED);
                        }
                        unpayedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.NOT_PAYED);
                    }
                    case CONSULT_PAYED_RESERVATION -> {
                        List<Reserva> unpayedReservations; //Lista para colocar na estrutura de comunicação
                        if (role == Role.ADMIN) {
                            unpayedReservations = dbComm.consultasReservadasAguardamPagamentoByUser(Payment.PAYED);
                        }
                        unpayedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.PAYED);
                    }
                    case CONSULT_SPECTACLE -> {

                    }
                    case CHOOSE_SPECTACLE_24H -> {
                        List<Espetaculo> espetaculos = dbComm.getEspetaculosAfter24Hours();
                    }
                    case SUBMIT_RESERVATION -> {
                        //Verificação
                    }
                    case CANCEL_RESERVATION -> {
                        //Verificação id reserva
                        int idReserva = 0; //TODO colocar reserva na estrutura de comunicação
                        Query query = dbComm.deleteReservaNotPayed(idReserva);
                        if (startUpdateRoutine(query, internalInfo)) {
                            dbComm.executeUserQuery(query);
                        } else {
                            //TODO enviar mensagem a cliente: impossel realizar transação, tente mais tarde
                        }
                    }
                    case ADD_SPECTACLE -> {
                        if (role == Role.ADMIN) {

                        }
                    }
                    case DELETE_SPECTACLE -> {
                    }
                    case LOGOUT -> {
                        Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.NOT_AUTHENTICATED);
                        if (startUpdateRoutine(query, internalInfo)) {
                            dbComm.executeUserQuery(query);
                        } else {
                            //TODO enviar mensagem a cliente: impossel realizar transação, tente mais tarde
                        }
                    }
                }
            } else {
                switch (msgClient.getAction()) {
                    case REGISTER_USER -> {

                    }

                }
            }
            dbComm.close();
        }catch (SQLException e){
            System.out.println(Arrays.toString(e.getStackTrace()));
        }


    }

    /**
     * Method to execute the server's Prepare message exchange routine with the other
     * servers to secure database consistency.
     * @param query query with current DB version, client request and current timestamp
     * @param internalInfo
     * @return true if client request can be made.
     */
    private boolean startUpdateRoutine(Query query, InternalInfo internalInfo) {
        boolean repeat = true;
        int confirmationCounter = 1;
        Set<Prepare> confirmationList = new HashSet<>();
        //Mudar estado para UPDATE
        synchronized (internalInfo){
            internalInfo.setStatus(Status.UPDATING);
        }
        //Abrir socket UPD auto para confirmações
        try {
            DatagramSocket confirmationSocket = new DatagramSocket(0);

            //Enviar PREPARE por MC
            Prepare prepareMsg = new Prepare(
                    TypeOfMulticastMsg.PREPARE,
                    query,
                    query.getNumVersion(),
                    internalInfo.getIp(),
                    confirmationSocket.getLocalPort()
            );
            byte[] bytes = new byte[10000];
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            ObjectStream objectStream = new ObjectStream();
            objectStream.writeObject(dp, prepareMsg);

            while (repeat) {
                //Se não estiverem todas, reenviar PREPARE por MC
                internalInfo.getMulticastSocket().send(dp);
                try {
                    //Esperar confirmações com timeout 1 sec
                    confirmationSocket.setSoTimeout(1000);
                    while (true) {
                        confirmationSocket.receive(dp);
                        Prepare confirmation = objectStream.readObject(dp, Prepare.class);
                        confirmationList.add(confirmation);
                    }
                } catch (SocketException e) {
                    //Verificar confirmações com lista de servidores
                    if(confirmationCounter == 0){
                        break;
                    }else if(verifyConfirmations(confirmationList))
                        repeat = false;
                    else {
                        confirmationList.clear();
                        confirmationCounter--;
                    }
                }
            }

            //Verificar confirmações com lista de servidores
            if(verifyConfirmations(confirmationList)){
                //Se estiverem todos enviar COMMIT
                try {
                    sendCommit(dp);
                } catch (IOException e) {
                    System.out.println("[AttendClientThread] - error on updateRoutine - could not send commit: "+ e.getMessage());
                    return false;
                }
                //Executar alteração
                return true;
            }
            //Se não estiverem todas enviar ABORT
            sendAbort(dp);
            //Cancelar a operação e informar utilizador

        } catch (SocketException e) {
            System.out.println("[AttendClientThread] - error on updateRoutine - could not open confirmation socket: "+ e.getMessage());
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - error on updateRoutine - could not send prepare: "+ e.getMessage());
        }
        return false;
    }
    /**
     * Method to send an Abort message through Multicast
     * @param dp Datagrampacket created for multicast message transmition
     * @throws IOException when it fails sending the packet
     */
    private void sendAbort(DatagramPacket dp) throws IOException {
        Abort abortMsg = new Abort();
        ObjectStream objectStream = new ObjectStream();
        objectStream.writeObject(dp,abortMsg);
        internalInfo.getMulticastSocket().send(dp);
    }

    /**
     * Method to send a Commit message through Multicast
     * @param dp Datagrampacket created for multicast message transmition
     * @throws IOException when it fails sending the packet
     */
    private void sendCommit(DatagramPacket dp) throws IOException {
        Commit commitMsg = new Commit();
        ObjectStream os = new ObjectStream();
        os.writeObject(dp,commitMsg);
        internalInfo.getMulticastSocket().send(dp);
    }

    private boolean verifyConfirmations(Set<Prepare> confirmationList) {
        if(confirmationList.size() == internalInfo.getHeatBeats().size()) {
            int count = 0;
            for (Prepare p : confirmationList) {
                for (HeartBeat hb : internalInfo.getHeatBeats())
                    if (p.getIp().equals(hb.getIp()) && p.getPortUdpClients() == hb.getPortUdp()) {
                        count++;
                        break;
                    }
            }
            if (count == internalInfo.getHeatBeats().size())
                return true;
        }
        return false;
    }

    private void closeClient() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ClientMSG ansMsg = new ClientMSG();
            //TODO remover o proprio heartbeat
            ansMsg.setServerList(internalInfo.getHeatBeats());
            oos.writeObject(ansMsg);
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - failed to send server list" + e.getMessage());
        }
    }



    private void doRegister(ClientMSG msgClient, DBCommunicationManager dbComm, ObjectOutputStream oos) throws SQLException, IOException {
        Register r = (Register) msgClient;
        if (dbComm.existsUserByUsernameOrName(r.getNome(), r.getUserName())) {
            Query query = dbComm.getRegisterUserQuery(r.getUserName(), r.getNome(), r.getPassword());
            if (startUpdateRoutine(query, internalInfo)) {
                dbComm.executeUserQuery(query);
            } else {
                ClientMSG msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
                oos.writeObject(msg);
            }
        } else {
            ClientMSG msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
            oos.writeObject(msg);
        }
    }

    private void doLogin(ClientMSG msgClient, DBCommunicationManager dbComm) throws SQLException {
        if (dbComm.checkUserLogin(msgClient.getUser().getUsername(), msgClient.getUser().getPassword())) {
            Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.AUTHENTICATED);
            if (startUpdateRoutine(query, internalInfo)) {
                dbComm.executeUserQuery(query);
            } else {
                //TODO enviar mensagem a cliente: impossel realizar transação, tente mais tarde
            }
        } else {
            //TODO enviar mensagem a cliente: invalid username or password
        }
    }

}
