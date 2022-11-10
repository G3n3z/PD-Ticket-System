package com.isec.pd22.server.threads;

import com.isec.pd22.enums.*;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.*;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.*;
import com.isec.pd22.server.models.*;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBCommunicationManager;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class AttendClientThread extends Thread{
    private Socket clientSocket;
    private InternalInfo internalInfo;
    private DBCommunicationManager dbComm;
    private DBVersionManager dbVersionManager;
    private Role role;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    FileOutputStream fos;
    Connection connection;
    boolean keepGoing = true;
    public AttendClientThread(Socket clientSocket, InternalInfo internalInfo, Connection connection) {
        this.clientSocket = clientSocket;
        this.internalInfo = internalInfo;
        this.connection = connection;
    }

    @Override
    public void run() {


        keepGoing = openStreams();
        dbVersionManager = new DBVersionManager(connection);
        dbComm = new DBCommunicationManager(internalInfo, connection);
        while (keepGoing) {
            try {
                ClientMSG clientMsg = (ClientMSG) ois.readObject();

                switch (internalInfo.getStatus())
                {
                    case AVAILABLE -> {handleClientRequest(clientMsg);}
                    case UPDATING -> {
                        internalInfo.lock.lock();
                        internalInfo.condition.await();
                        internalInfo.lock.unlock();
                        handleClientRequest(clientMsg);
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
                System.out.println("[AttendClientThread] - failed comunication with client: "+ e.getMessage());
                keepGoing = false;
            } catch (InterruptedException e) {
                System.out.println("[AttendClientThread] - server finished updating: " + e.getMessage());
            }
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - error on closing client socket: "+ e.getMessage());
        }
        synchronized (internalInfo) {
            internalInfo.decrementNumClients();
        }
        System.out.println("Sai da thread do cliente");
    }

    private boolean openStreams() {
        try{
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ois = new ObjectInputStream(clientSocket.getInputStream());
        }catch (IOException e){
            System.out.println("[AttendClientThread] - Nao foi possivel abrir o Streams");
            return false;
        }
        return true;
    }

    private void handleClientRequest(ClientMSG msgClient){
        ClientMSG ansMsg = new ClientMSG();
        try {


            switch (msgClient.getAction()){
                case REGISTER_USER -> {
                    doRegister(msgClient, dbComm);
                }
                case LOGIN -> {
                    doLogin(msgClient, dbComm);
                }
                case EXIT -> {
                   exitClient(msgClient);
                }
                default -> actionsLogged(msgClient, dbComm);
            }

        } catch (SQLException | IOException e) {
            ansMsg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
            System.out.println("[AttendClientThread] - failed to initialize DB communication: "+ e.getMessage());
        }
    }

    private void exitClient(ClientMSG msgClient) throws SQLException {
        System.out.println("Cliente saiu");
        keepGoing = false;
        if (msgClient.getUser() != null){
            Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.NOT_AUTHENTICATED);
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
            }
        }
    }


    private void actionsLogged(ClientMSG msgClient, DBCommunicationManager dbComm) {
        try {
            ClientMSG msg = null;
            User user = dbComm.getUser(msgClient.getUser().getUsername());
            if (user != null && user.getAuthenticated() == Authenticated.AUTHENTICATED) {
                switch (msgClient.getAction()) {
                    case EDIT_USER -> {
                        if (dbComm.canEditUser(msgClient.getUser().getNome(), msgClient.getUser().getUsername())) {
                            Query query = dbComm.editUtilizador(
                                    msgClient.getUser().getUsername(),
                                    msgClient.getUser().getNome(),
                                    msgClient.getUser().getPassword()
                            );
                            if (startUpdateRoutine(query, internalInfo)) {
                                dbVersionManager.insertQuery(query);
                            } else {
                                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
                            }
                        }else{
                            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
                        }
                    }
                    case CONSULT_UNPAYED_RESERVATION -> {
                        List<Reserva> unpayedReservations;
                        unpayedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.NOT_PAYED);
                        msg = new RequestListReservas(ClientsPayloadType.RESERVAS_RESPONSE, unpayedReservations);

                    }
                    case CONSULT_PAYED_RESERVATION -> {
                        List<Reserva> payedReservations;
                        payedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.PAYED);
                        msg = new RequestListReservas(ClientsPayloadType.RESERVAS_RESPONSE, payedReservations);
                    }
                    case CONSULT_SPECTACLE -> {
                        List<Espetaculo> list = dbComm.getEspetaculosAfter24Hours();
                        msg = new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE, list);
                    }
                    case CHOOSE_SPECTACLE_24H -> {
                        Espetaculos espetaculos = (Espetaculos) msgClient;
                        List<Espetaculo> list = dbComm.getEspetaculoWithFilters(espetaculos);
                        msg = new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE,list);
                    }
                    case SUBMIT_RESERVATION -> {
                        ListPlaces list = (ListPlaces) msgClient;
                        if(dbComm.canSubmitReservations(list)){
                            Query query = dbComm.submitReservations(list);
                            if (startUpdateRoutine(query, internalInfo)) {
                                dbVersionManager.insertQuery(query);

                                //Obter id da reserva inserida e iniciar o timertask que contrala o pagamento
                                int lastId = dbComm.getLastId("reserva");
                                System.out.println("[lastId reserva] = " + lastId);
                                Timer timer = new Timer(true);
                                timer.schedule(new ControlPaymentTask(lastId,connection,internalInfo,timer),Constants.PAYMENT_TIMER);

                                msg = new ClientMSG(ClientActions.SUBMIT_RESERVATION);
                                msg.setClientsPayloadType(ClientsPayloadType.ACTION_SUCCEDED);
                            } else {
                                msg = new ClientMSG(ClientActions.SUBMIT_RESERVATION);
                                msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
                            }
                        }else {
                            msg = new ClientMSG(ClientActions.SUBMIT_RESERVATION);
                            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
                        }

                    }
                    case GET_RESERVS -> {
                        List<Reserva> reservas = dbComm.getAllReservas();
                        msg = new RequestListReservas(ClientsPayloadType.RESERVAS_RESPONSE,reservas);
                        msg.setAction(ClientActions.GET_RESERVS);
                    }
                    case CANCEL_RESERVATION -> {
                        RequestListReservas list = (RequestListReservas) msgClient;
                        if(dbComm.canCancelReservation(list.getReservas().get(0).getIdReserva())) {
                            Query query = dbComm.deleteReservaNotPayed(list.getReservas().get(0).getIdReserva());
                            if (startUpdateRoutine(query, internalInfo)) {
                                dbVersionManager.insertQuery(query);
                                msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
                                msg.setClientsPayloadType(ClientsPayloadType.ACTION_SUCCEDED);
                            } else {
                                msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
                                msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
                            }
                        }else{
                            msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
                            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
                        }
                    }
                    case ADD_SPECTACLE -> {
                        if (user.getRole() == Role.ADMIN) {
                            if(readFile(msgClient)){
                                Query query = importToBD((FileUpload) msgClient);
                                if (startUpdateRoutine(query, internalInfo)) {
                                    dbVersionManager.insertQuery(query);
                                    msg = new ClientMSG(ClientsPayloadType.FILE_UPDATED);
                                } else {
                                    msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
                                    msg.setAction(ClientActions.ADD_SPECTACLE);
                                }
                            }else{
                                msg = new ClientMSG(ClientsPayloadType.PART_OF_FILE_UPLOADED);
                            }
                        }
                    }
                    case DELETE_SPECTACLE -> {
                        if(user.getRole() == Role.ADMIN){
                            Espetaculos list = (Espetaculos) msgClient;
                            if(dbComm.canRemoveEspecatulo(list.getEspetaculos().get(0).getIdEspetaculo())){
                                Query  query = dbComm.deleteSpectacle(list.getEspetaculos().get(0).getIdEspetaculo());
                                if (startUpdateRoutine(query, internalInfo)) {
                                    dbVersionManager.insertQuery(query);
                                    msg = new ClientMSG(ClientActions.DELETE_SPECTACLE);
                                    msg.setClientsPayloadType(ClientsPayloadType.CONSULT_SPECTACLE);
                                } else {
                                    msg = new ClientMSG(ClientActions.DELETE_SPECTACLE);
                                    msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
                                }
                            }else {
                                msg = new ClientMSG(ClientActions.DELETE_SPECTACLE);
                                msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
                            }
                        }
                    }
                    case CONSULT_SPECTACLE_DETAILS -> {
                        RequestDetailsEspetaculo req = (RequestDetailsEspetaculo) msgClient;
                        Espetaculo e = dbComm.getEspetaculoDetailsByIdWithLugares(req.getEspetaculo().getIdEspetaculo());
                        req.setEspetaculo(e);
                        req.setClientsPayloadType(ClientsPayloadType.SPECTACLE_DETAILS);
                        msg = req;
                    }
                    case LOGOUT -> {
                        Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.NOT_AUTHENTICATED);
                        if (startUpdateRoutine(query, internalInfo)) {
                            dbVersionManager.insertQuery(query);
                            msg = new ClientMSG(ClientsPayloadType.LOGOUT);
                        } else {
                            msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
                            msg.setAction(ClientActions.LOGOUT);
                        }
                    }
                }
            } else {
               msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
            }
            sendMessage(msg);
        }catch (SQLException e){
            System.out.println(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private boolean readFile(ClientMSG msgClient) throws IOException {
        FileUpload fileUpload = (FileUpload) msgClient;
        if(fos == null){
            fos = new FileOutputStream(Constants.FILES_DIR_PATH + fileUpload.getName());
        }
        if(fileUpload.isLast()){
            fos.close();
            fos = null;
        }else {
            fos.write(fileUpload.getBytes(), 0, fileUpload.getSizeBytes());
       }
        return fileUpload.isLast();
    }

    private Query importToBD(FileUpload fileUpload) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(Constants.FILES_DIR_PATH+fileUpload.getName()));
        Espetaculo espetaculo = readHeaders(br);
        List<Lugar> lugares = readFilas(br);
        br.close();
        Query query = dbComm.insertEspetaculo(espetaculo);
        dbComm.insertLugares(lugares, query);
        return query;
    }

    private List<Lugar> readFilas(BufferedReader br)  {
        char fila;
        List<Lugar> lugars = new ArrayList<>();
        while (true){
            try {
                String line = br.readLine();
                if(line == null){
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("\"Fila")){
                    continue;
                }
                String [] args = line.split(";");
                args = retiraAspas(args);
                if(args.length < 2){
                    continue;
                }
                readLineLugares(args, lugars);
            } catch (IOException e) {
                break;
            }
        }
        return lugars;
    }

    private void readLineLugares(String[] args, List<Lugar> lugars) {
        String fila = args[0]; String assento; double preco;
        for (int i = 1; i < args.length; i++) {
            String []lugar = args[i].split(":");
            if(lugar.length != 2){
                continue;
            }
            assento = lugar[0].replaceAll("\"", "");
            preco = Double.parseDouble(lugar[1].replaceAll("\"", ""));
            lugars.add(new Lugar(fila, assento, preco));
        }
    }

    private Espetaculo readHeaders( BufferedReader br) throws IOException {
        int index = 0;
        String designacao = null, tipo = null, data, local = null, localidade = null, pais = null, classificacao = null;
        int dia = 0;
        int mes = 0;
        int ano = 0, hora = 0, minutos = 0, duracao = 0;
        while(index <= 8){
            String line = br.readLine();
            String [] arguments = line.split(";");
            arguments = retiraAspas(arguments);
            switch (index){
                case 0 -> {
                    if(arguments[0] == null || !arguments[0].equals("Designação") || arguments.length != 2){
                        throw new ServerException("Designação nao encontrada");
                    }
                    designacao = arguments[1];
                }
                case 1 -> {
                    if(arguments[0] == null || !arguments[0].equals("Tipo") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    tipo = arguments[1];
                }
                case 2 -> {
                    if(arguments[0] == null || !arguments[0].equals("Data") || arguments.length != 4){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    dia = Integer.parseInt(arguments[1]);
                    mes = Integer.parseInt(arguments[2]);
                    ano = Integer.parseInt(arguments[3]);
                }
                case 3 -> {
                    if(arguments[0] == null || !arguments[0].equals("Hora") || arguments.length != 3){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    hora = Integer.parseInt(arguments[1]);
                    minutos = Integer.parseInt(arguments[2]);

                }
                case 4 -> {
                    if(arguments[0] == null || !arguments[0].equals("Duração") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    duracao = Integer.parseInt(arguments[1]);
                }
                case 5 -> {
                    if(arguments[0] == null || !arguments[0].equals("Local") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    local = arguments[1];
                }
                case 6 -> {
                    if(arguments[0] == null || !arguments[0].equals("Localidade") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    localidade = arguments[1];
                }
                case 7 -> {
                    if(arguments[0] == null || !arguments[0].equals("País") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    pais = arguments[1];
                }
                case 8 -> {
                    if(arguments[0] == null || !arguments[0].equals("Classificação etária") || arguments.length != 2){
                        throw new ServerException("Tipo nao encontrada");
                    }
                    classificacao = arguments[1];
                }

            }
            index++;
        }
        return new Espetaculo(designacao, tipo, new Date(ano, mes, dia, hora, minutos), duracao, local,
                localidade, pais, classificacao);

    }

    private String[] retiraAspas(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = arguments[i].replaceAll("\"", "");
        }
        return arguments;
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
            byte[] bytes = new byte[10000];
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            while (repeat) {
                Prepare prepareMsg = new Prepare(
                        TypeOfMulticastMsg.PREPARE,
                        query,
                        query.getNumVersion(),
                        internalInfo.getIp(),
                        confirmationSocket.getLocalPort()
                );

                ObjectStream objectStream = new ObjectStream();
                objectStream.writeObject(dp, prepareMsg);

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
                } catch (SocketException | SocketTimeoutException e) {
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

        }  catch (IOException e) {
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
        Abort abortMsg = new Abort(TypeOfMulticastMsg.ABORT);
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
        Commit commitMsg = new Commit(TypeOfMulticastMsg.COMMIT, internalInfo.getIp(), internalInfo.getPortUdp());
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
            Set<HeartBeat> list = new HashSet<>();
            list.addAll(internalInfo.getHeatBeats());
            list.remove(new HeartBeat(internalInfo.getIp(),internalInfo.getPortUdp()));
            ansMsg.setServerList(list);
            oos.writeObject(ansMsg);
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - failed to send server list" + e.getMessage());
        }
    }



    private void doRegister(ClientMSG msgClient, DBCommunicationManager dbComm) throws SQLException, IOException {
        Register r = (Register) msgClient;
        ClientMSG msg;
        if (dbComm.existsUserByUsernameOrName(r.getNome(), r.getUserName())) {
            Query query = dbComm.getRegisterUserQuery(r.getUserName(), r.getNome(), r.getPassword());
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                msg = new ClientMSG(ClientsPayloadType.USER_REGISTER);
            } else {
                msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
            }
        } else {
            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);

        }
        oos.writeObject(msg);
    }

    private void doLogin(ClientMSG msgClient, DBCommunicationManager dbComm) throws SQLException, IOException {
        ClientMSG msg;
        User u = dbComm.getUser(msgClient.getUser().getUsername());
        if (u != null && u.getPassword().equals(msgClient.getUser().getPassword())) {
            Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.AUTHENTICATED);
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                msg = new ClientMSG(ClientsPayloadType.LOGGED_IN);
                msg.setUser(new User(u.getIdUser(),u.getRole(),u.getUsername(), u.getNome()));
            } else {
                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            }
        } else {

            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
        }
        oos.writeObject(msg);
    }


    public void sendMessage(ClientMSG msg) throws IOException {
        if(msg == null){
            System.out.println("Tentativa de envio de mengsane a null ");
            return;
        }
        oos.writeObject(msg);
    }

}
