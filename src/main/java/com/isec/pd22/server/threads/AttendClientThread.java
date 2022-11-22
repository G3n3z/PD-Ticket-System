package com.isec.pd22.server.threads;

import com.isec.pd22.enums.*;
import com.isec.pd22.interfaces.Observer;
import com.isec.pd22.payload.*;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.*;
import com.isec.pd22.server.models.*;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBCommunicationManager;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class AttendClientThread extends Thread implements Observer {
    private Socket clientSocket;
    private InternalInfo internalInfo;
    private DBCommunicationManager dbComm;
    private DBVersionManager dbVersionManager;
    private Role role;
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    FileOutputStream fos;
    Connection connection;
    ClientMSG lastMessageReceive;
    Timer timer;
    boolean keepGoing = true;
    public AttendClientThread(Socket clientSocket, InternalInfo internalInfo, Connection connection) {
        this.clientSocket = clientSocket;
        this.internalInfo = internalInfo;
        this.connection = connection;
        internalInfo.addObserver(this);
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
        }finally {
            internalInfo.removeObserver(this);
        }
        synchronized (internalInfo) {
            internalInfo.decrementNumClients();
        }
        internalInfo.removeClientThread(this);
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

    private void handleClientRequest(ClientMSG msgClient) throws IOException {
        ClientMSG ansMsg = new ClientMSG();
        try {
            updateLastMessage(msgClient);
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
                case LOGOUT -> logout(msgClient);
                default -> actionsLogged(msgClient, dbComm);
            }

        } catch (SQLException | IOException e) {
            ansMsg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
            e.printStackTrace();
            System.out.println("[AttendClientThread] - communication: "+ e.getMessage());
            oos.writeUnshared(ansMsg);
        }
    }

    private void exitClient(ClientMSG msgClient) throws SQLException, IOException {
        System.out.println("Cliente saiu");
        keepGoing = false;
        if (msgClient.getUser() != null){
            Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.NOT_AUTHENTICATED);
            if(startUpdateRoutine(query, internalInfo)){
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
            }else {
                sendAbort();
            }


        }
    }


    private void actionsLogged(ClientMSG msgClient, DBCommunicationManager dbComm) throws IOException {
        try {
            ClientMSG msg = null;
            User user = dbComm.getUser(msgClient.getUser().getUsername());
            if (user != null && user.getAuthenticated() == Authenticated.AUTHENTICATED) {
                switch (msgClient.getAction()) {
                    case EDIT_USER -> msg = editUser(msgClient);
                    case CONSULT_UNPAYED_RESERVATION -> msg = consultUnpayedReservation(msgClient);
                    case CONSULT_PAYED_RESERVATION -> msg = consultPayedReservation(msgClient);
                    case CONSULT_SPECTACLE -> msg = choose_spectacle(msgClient);
                    case CHOOSE_SPECTACLE_24H -> msg = choose_spectacle_24h(msgClient);
                    case SUBMIT_RESERVATION -> msg = submitReservation(msgClient);
                    case GET_RESERVS -> msg = getReservs();
                    case CANCEL_RESERVATION -> msg = cancelReservation(msgClient);
                    case ADD_SPECTACLE -> msg = addSpectacule(msgClient, user);
                    case DELETE_SPECTACLE -> msg = deleteSpectacle(msgClient, user);
                    case SWITCH_VISIBILITY -> msg = switchSpectacleVisibility(msgClient, user);
                    case CONSULT_SPECTACLE_DETAILS -> msg = consult_spectacle_details(msgClient);
                    case PAY_RESERVATION -> msg = payReservation(msgClient);
                }
            } else {
               msg = new ClientMSG(msgClient.getAction(), ClientsPayloadType.NOT_AUTHENTICATED);
            }
            sendMessage(msg);
        } catch (SQLException e){
            System.out.println(Arrays.toString(e.getStackTrace()));
        } catch (IOException e) {
            sendAbort();
            e.printStackTrace();
        }
    }

    private ClientMSG payReservation(ClientMSG msgClient) throws SQLException, IOException {
        RequestListReservas listReservas = (RequestListReservas) msgClient;
        Query query = dbComm.payReservations(listReservas);
        ClientMSG msg;
        if(query != null){
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
                msg = new ClientMSG(ClientsPayloadType.ACTION_SUCCEDED);

            } else {
                sendAbort();
                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            }
        }else{
            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
        }
        return msg;
    }

    private ClientMSG editUser(ClientMSG msgClient) throws SQLException, IOException {
        ClientMSG msg;
        if (dbComm.canEditUser(msgClient.getUser().getNome(), msgClient.getUser().getUsername())) {
            EditUser editUser = (EditUser) msgClient;
            Query query = dbComm.editUtilizador(
                    editUser.getUser().getIdUser(),
                    editUser.getUsername(),
                    editUser.getNome(),
                    editUser.getPassword() == null ? null : BCrypt.hashpw(editUser.getPassword(), BCrypt.gensalt())
            );
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
                msg = new ClientMSG(ClientsPayloadType.ACTION_SUCCEDED);
            } else {
                sendAbort();
                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            }
        }else{
            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);
        }
        return msg;
    }

    private ClientMSG consultUnpayedReservation(ClientMSG msgClient) {
        List<Reserva> unpayedReservations;
        unpayedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.NOT_PAYED);
        return new RequestListReservas(msgClient.getAction(),ClientsPayloadType.RESERVAS_RESPONSE, unpayedReservations);
    }

    private ClientMSG consultPayedReservation(ClientMSG msgClient) {
        List<Reserva> payedReservations;
        payedReservations = dbComm.consultasReservadasByUser(msgClient.getUser().getIdUser(), Payment.PAYED);
        return new RequestListReservas(msgClient.getAction(),ClientsPayloadType.RESERVAS_RESPONSE, payedReservations);
    }

    private ClientMSG choose_spectacle(ClientMSG msgClient) {
        Espetaculos espetaculos = (Espetaculos) msgClient;
        List<Espetaculo> list = dbComm.getEspetaculoWithFilters(espetaculos);
        return new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE,list);
    }

    private ClientMSG choose_spectacle_24h(ClientMSG msgClient) {
        List<Espetaculo> list = dbComm.getEspetaculosAfter24Hours();
        return new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE, list);
    }

    private ClientMSG submitReservation(ClientMSG msgClient) throws SQLException, IOException {
        ClientMSG msg;
        ListPlaces list = (ListPlaces) msgClient;
        if(dbComm.canSubmitReservations(list) && dbComm.isSpectacleVisible(list)){
            Query query = dbComm.submitReservations(list);
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                    System.out.println(internalInfo.getNumDB());
                }
                sendCommit();

                //Obter id da reserva inserida e iniciar o timertask que contrala o pagamento
                int lastId = dbComm.getLastId("reserva");
                timer = new Timer(true);
                timer.schedule(new ControlPaymentTask(lastId,connection,internalInfo,timer),Constants.PAYMENT_TIMER);
                list.getPlaces().forEach(lugar -> lugar.getReserva().setIdReserva(lastId));
                msg = new ListPlaces(ClientActions.SUBMIT_RESERVATION, ClientsPayloadType.SUBMIT_RESERVATION_NOT_PAYED,
                        list.getPlaces());

            } else {
                msg = new ClientMSG(ClientActions.SUBMIT_RESERVATION);
                msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
                sendAbort();
            }
        }else {
            msg = new ClientMSG(ClientActions.SUBMIT_RESERVATION);
            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
            msg.setMessage("Espetaculo não visível para reservas");
        }
        return msg;
    }

    private ClientMSG getReservs() {
        ClientMSG msg;
        List<Reserva> reservas = dbComm.getAllReservas();
        msg = new RequestListReservas(ClientsPayloadType.RESERVAS_RESPONSE,reservas);
        msg.setAction(ClientActions.GET_RESERVS);
        return msg;
    }

    private ClientMSG cancelReservation(ClientMSG msgClient) throws SQLException, IOException {
        ClientMSG msg;
        RequestListReservas list = (RequestListReservas) msgClient;
        if(dbComm.canCancelReservation(list.getReservas().get(0).getIdReserva())) {
            Query query = dbComm.deleteReservaNotPayed(list.getReservas().get(0).getIdReserva());
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
                msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
                msg.setClientsPayloadType(ClientsPayloadType.ACTION_SUCCEDED);
                //TODO: TESTE
                return null;
            } else {
                sendAbort();
                msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
                msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
            }
        }else{
            msg = new ClientMSG(ClientActions.CANCEL_RESERVATION);
            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
        }
        return msg;
    }


    private ClientMSG addSpectacule(ClientMSG msgClient, User user) throws IOException, SQLException {
        if (user.getRole() != Role.ADMIN) {
            return new ClientMSG(ClientActions.DELETE_SPECTACLE, ClientsPayloadType.BAD_REQUEST);
        }

        if(!readFile(msgClient)) {
            return new ClientMSG(ClientsPayloadType.PART_OF_FILE_UPLOADED);
        }

        Query query;

        try {
            query = importToBD((FileUpload) msgClient);
        } catch (Exception e) {
            return new ClientMSG(ClientActions.DELETE_SPECTACLE, ClientsPayloadType.BAD_REQUEST, e.getMessage());
        }


        if (startUpdateRoutine(query, internalInfo)) {
            dbVersionManager.insertQuery(query);
            synchronized (internalInfo){
                internalInfo.setNumDB(internalInfo.getNumDB()+1);
            }
            sendCommit();
            return new ClientMSG(ClientsPayloadType.FILE_UPDATED);
        }else {
            sendAbort();
        }

        ClientMSG msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
        msg.setAction(ClientActions.ADD_SPECTACLE);

        return msg;
    }

    private ClientMSG deleteSpectacle(ClientMSG msgClient, User user) throws SQLException, IOException {
        RequestDetailsEspetaculo msg;
        //TODO mudar isto para um espetaculo
        if(user.getRole() == Role.ADMIN){
            RequestDetailsEspetaculo espetaculo = (RequestDetailsEspetaculo) msgClient;
            if(dbComm.canRemoveEspecatulo(espetaculo.getEspetaculo().getIdEspetaculo())){
                Query  query = dbComm.deleteSpectacle(espetaculo.getEspetaculo().getIdEspetaculo());
                if (startUpdateRoutine(query, internalInfo)) {
                    dbVersionManager.insertQuery(query);
                    synchronized (internalInfo){
                        internalInfo.setNumDB(internalInfo.getNumDB()+1);
                    }
                    sendCommit();
                    msg = new RequestDetailsEspetaculo(ClientActions.DELETE_SPECTACLE);
                    msg.setClientsPayloadType(ClientsPayloadType.DELETE_SPECTACLE);
                    msg.setEspetaculo(espetaculo.getEspetaculo());
                } else {
                    sendAbort();
                    msg = new RequestDetailsEspetaculo(ClientActions.DELETE_SPECTACLE);
                    msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
                }
            }else {
                msg = new RequestDetailsEspetaculo(ClientActions.DELETE_SPECTACLE);
                msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
                msg.setMessage("Não pode remover um espetaculo com reservas");
            }
        }else {
            msg = new RequestDetailsEspetaculo(ClientActions.DELETE_SPECTACLE);
            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
        }
        return msg;
    }

    private ClientMSG switchSpectacleVisibility(ClientMSG msgClient, User user) throws IOException {
        Espetaculos msg = new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE);
        if (user.getRole() != Role.ADMIN) {
            return new ClientMSG(ClientActions.CONSULT_SPECTACLE, ClientsPayloadType.BAD_REQUEST);
        }
        RequestDetailsEspetaculo espetaculo = (RequestDetailsEspetaculo) msgClient;
        if(dbComm.canEditSpectacle(espetaculo.getEspetaculo().getIdEspetaculo())) {
            Query query = dbComm.switchSpectacleVisibility(espetaculo.getEspetaculo());

            if (startUpdateRoutine(query, internalInfo)) {
                try {
                    dbVersionManager.insertQuery(query);
                    synchronized (internalInfo){
                        internalInfo.setNumDB(internalInfo.getNumDB()+1);
                    }
                    sendCommit();
                } catch (SQLException e) {
                    System.out.println("[AttendClientThread] - switchSpectacleVisibility - could not change visibility: " + e.getMessage());
                    sendAbort();
                } catch (IOException e) {
                    System.out.println("[AttendClientThread] - switchSpectacleVisibility - could not send commit: " + e.getMessage());
                }
                return null;
            } else {
                try {
                    sendAbort();
                } catch (IOException e) {
                    System.out.println("[AttendClientThread] - switchSpectacleVisibility - could not send abort: " + e.getMessage());
                }
                msg.setClientsPayloadType(ClientsPayloadType.TRY_LATER);
            }
        }else {
            msg.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
            msg.setMessage("Impossível mudar visibilidade.\nEspetáculo contem reservas");
        }
        return msg;
    }

    private ClientMSG consult_spectacle_details(ClientMSG msgClient) {
        RequestDetailsEspetaculo req = (RequestDetailsEspetaculo) msgClient;
        Espetaculo e = dbComm.getEspetaculoDetailsByIdWithLugares(req.getEspetaculo().getIdEspetaculo());
        if (e == null){
            req.setClientsPayloadType(ClientsPayloadType.BAD_REQUEST);
            req.setMessage("Espetaculo indisponivel");
        }else {
            req.setEspetaculo(e);
            req.setClientsPayloadType(ClientsPayloadType.SPECTACLE_DETAILS);
        }
        return req;
    }

    private void logout(ClientMSG msgClient) throws SQLException, IOException {
        ClientMSG msg;
        Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.NOT_AUTHENTICATED);
        if (startUpdateRoutine(query, internalInfo)) {
            dbVersionManager.insertQuery(query);
            synchronized (internalInfo){
                internalInfo.setNumDB(internalInfo.getNumDB()+1);
            }
            sendCommit();
            msg = new ClientMSG(ClientsPayloadType.LOGOUT);
        } else {
            sendAbort();
            msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            msg.setAction(ClientActions.LOGOUT);
        }
        sendMessage(msg);
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

    private Query importToBD(FileUpload fileUpload) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(Constants.FILES_DIR_PATH+fileUpload.getName()));

        ShowFromFileModel show = new ShowFromFileModel(Constants.FILES_DIR_PATH+fileUpload.getName());

        br.close();
        Query query = dbComm.insertEspetaculo(show.getShow());
        dbComm.insertLugares(show.getSeats(), query);
        return query;
    }

    /**
     * Method to execute the server's Prepare message exchange routine with the other
     * servers to secure database consistency.
     * @param query query with current DB version, client request and current timestamp
     * @param internalInfo
     * @return true if client request can be made.
     */
    private boolean startUpdateRoutine(Query query, InternalInfo internalInfo){
        boolean repeat = true;
        int confirmationCounter = 1;
        Set<Prepare> confirmationList = new HashSet<>();
        byte[] bytes = new byte[10000];
        //Mudar estado para UPDATE
        synchronized (internalInfo){
            internalInfo.setStatus(Status.UPDATING);
        }
        //Abrir socket UPD auto para confirmações
        try {
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            DatagramSocket confirmationSocket = new DatagramSocket(0);
            sendPrepare(dp, query, confirmationList, confirmationSocket);

            return verifyConfirmations(confirmationList);
            //Verificar confirmações com lista de servidores
        }  catch (IOException e) {
            System.out.println("[AttendClientThread] - error on updateRoutine - could not send prepare: "+ e.getMessage());
            return false;
        }
    }


    private void sendPrepare(DatagramPacket dp, Query query, Set<Prepare> confirmationList, DatagramSocket confirmationSocket) throws IOException {
        //Enviar PREPARE por MC
        boolean repeat = true;
        int confirmationCounter = 1;


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
                    if (confirmationList.size() == internalInfo.getHeatBeats().size() && verifyConfirmations(confirmationList) ){
                        repeat = false;
                        break;
                    }
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
    }

    /**
     * Method to send an Abort message through Multicast
     * @throws IOException when it fails sending the packet
     */
    private void sendAbort() throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[1000], 1000, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
        Abort abortMsg = new Abort(TypeOfMulticastMsg.ABORT);
        ObjectStream objectStream = new ObjectStream();
        objectStream.writeObject(dp,abortMsg);
        internalInfo.getMulticastSocket().send(dp);
    }

    /**
     * Method to send a Commit message through Multicast
     * @throws IOException when it fails sending the packet
     */
    private void sendCommit() throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[1000], 1000, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
        Commit commitMsg = new Commit(TypeOfMulticastMsg.COMMIT, internalInfo.getIp(), internalInfo.getPortUdp());
        ObjectStream os = new ObjectStream();
        os.writeObject(dp,commitMsg);
        internalInfo.getMulticastSocket().send(dp);
    }

    private boolean verifyConfirmations(Set<Prepare> confirmationList) {
        synchronized (internalInfo.getHeatBeats()){
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
        }
        return false;
    }

    private void closeClient() {
        try {
            ClientMSG ansMsg = new ClientMSG();
            List<HeartBeat> list = new ArrayList<>(internalInfo.getOrderedHeatBeats().stream().filter(heartBeat -> heartBeat.getStatusServer() != Status.UNAVAILABLE).toList());
            list.removeIf(heartBeat -> heartBeat.getIp().equals(internalInfo.getIp()) && heartBeat.getPortUdp() == internalInfo.getPortUdp());
            ansMsg.setServerList(list);
            ansMsg.setClientsPayloadType(ClientsPayloadType.SHUTDOWN);
            oos.writeUnshared(ansMsg);
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - failed to send server list" + e.getMessage());
        }
    }



    private void doRegister(ClientMSG msgClient, DBCommunicationManager dbComm) throws SQLException, IOException {
        Register r = (Register) msgClient;
        ClientMSG msg;
        if (dbComm.existsUserByUsernameOrName(r.getNome(), r.getUserName(), r.getPassword())) {
            String encryptedPassword = BCrypt.hashpw(r.getPassword(), BCrypt.gensalt());

            Query query = dbComm.getRegisterUserQuery(r.getUserName(), r.getNome(), encryptedPassword);

            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
                msg = new ClientMSG(ClientsPayloadType.USER_REGISTER);
            } else {
                sendAbort();
                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            }
        } else {
            msg = new ClientMSG(ClientsPayloadType.BAD_REQUEST);

        }
        oos.writeUnshared(msg);
    }

    private void doLogin(ClientMSG msgClient, DBCommunicationManager dbComm) throws SQLException, IOException {
        ClientMSG msg;
        User u = dbComm.getUser(msgClient.getUser().getUsername());

        if (u != null && BCrypt.checkpw(msgClient.getUser().getPassword(), u.getPassword())) {
            Query query = dbComm.setAuthenticate(msgClient.getUser().getUsername(), Authenticated.AUTHENTICATED);
            if (startUpdateRoutine(query, internalInfo)) {
                dbVersionManager.insertQuery(query);
                synchronized (internalInfo){
                    internalInfo.setNumDB(internalInfo.getNumDB()+1);
                }
                sendCommit();
                msg = new ClientMSG(ClientActions.LOGIN, ClientsPayloadType.LOGGED_IN);
                msg.setUser(new User(u.getIdUser(),u.getRole(),u.getUsername(), u.getNome()));
            } else {
                sendAbort();
                msg = new ClientMSG(ClientsPayloadType.TRY_LATER);
            }
        } else {
            msg = new ClientMSG(ClientActions.LOGIN, ClientsPayloadType.BAD_REQUEST,"Username ou Password incorretos") ;
        }
        oos.writeUnshared(msg);
    }


    public void sendMessage(ClientMSG msg) throws IOException {
        if(msg == null){
            System.out.println("Tentativa de envio de mengsane a null ");
            return;
        }
        oos.writeUnshared(msg);
    }


    public void updateLastMessage(ClientMSG msg){
        lastMessageReceive =
            switch (msg.getAction()){
                case CONSULT_SPECTACLE, CONSULT_SPECTACLE_DETAILS, CHOOSE_SPECTACLE_24H,
                        CONSULT_PAYED_RESERVATION, CONSULT_UNPAYED_RESERVATION, GET_RESERVS -> msg;
                case EXIT, LOGOUT, LOGIN, EDIT_USER -> null;
                default -> lastMessageReceive;
            };

    }
    @Override
    public void update() throws IOException {
        if (lastMessageReceive != null) {
            System.out.println("Vou enviar mensagem - " + lastMessageReceive.getAction());
            handleClientRequest(lastMessageReceive);
        }
    }

    public void closeThread() {
        if(ois == null)
            return;
        closeClient();
        try {
            ois.close();
        } catch (IOException ignored) {
        }
    }
}
