package com.isec.pd22.server.threads;

import com.isec.pd22.enums.*;
import com.isec.pd22.exception.ServerException;
import com.isec.pd22.payload.*;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.FileUpload;
import com.isec.pd22.payload.tcp.Request.Register;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
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
                    case AVAILABLE -> {handleClientRequest(clientMsg, oos);}
                    case UPDATING -> {
                        //TODO enviar mensagem de update para o cliente
                        internalInfo.lock.lock();
                        internalInfo.condition.await();
                        internalInfo.lock.unlock();
                        handleClientRequest(clientMsg, oos);
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
                keepGoing = false;
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

    private void handleClientRequest(ClientMSG msgClient, ObjectOutputStream oos){
        try {

            ClientMSG ansMsg = new ClientMSG();

            switch (msgClient.getAction()){
                case REGISTER_USER -> {
                    doRegister(msgClient, dbComm, oos);
                }
                case LOGIN -> {
                    doLogin(msgClient, dbComm);
                }
                case EXIT -> {
                   exitClient(msgClient);
                }
                default -> actionsLogged(msgClient, oos, dbComm);
            }

        } catch (SQLException | IOException e) {
            System.out.println("[AttendClientThread] - failed to initialize DB communication: "+ e.getMessage());
        }

        //TODO preencher mensagem de retorno com resultado da query e enviar a cliente

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


    private void actionsLogged(ClientMSG msgClient, ObjectOutputStream oos, DBCommunicationManager dbComm) {
        try {
            //dbComm.isLogged(msgClient.getUser().getUsername());
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
                                //dbComm.executeUserQuery(query);
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
                        Espetaculos espetaculos = (Espetaculos) msgClient;
                        List<Espetaculo> list = dbComm.getEspetaculosAfter24Hours();
                        msg = new Espetaculos(ClientsPayloadType.CONSULT_SPECTACLE, list);
                    }
                    case CHOOSE_SPECTACLE_24H -> {
                        List<Espetaculo> espetaculos = dbComm.getEspetaculosAfter24Hours();
                    }
                    case SUBMIT_RESERVATION -> {
                        //Verificação
                    }
                    case GET_RESERVS -> {
                        RequestListReservas request = new RequestListReservas(ClientsPayloadType.RESERVAS_RESPONSE);
                        List<Reserva> reservas = dbComm.getAllReservas();
                        request.setReservas(reservas);
                        request.setAction(ClientActions.GET_RESERVS);
                    }
                    case CANCEL_RESERVATION -> {
                        //Verificação id reserva
                        int idReserva = 0; //TODO colocar reserva na estrutura de comunicação
                        Query query = dbComm.deleteReservaNotPayed(idReserva);
                        if (startUpdateRoutine(query, internalInfo)) {
                            //dbComm.executeUserQuery(query);
                        } else {
                            //TODO enviar mensagem a cliente: impossel realizar transação, tente mais tarde
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
               //TODO: Tentativa de acao de cliente nao ligado
            }
            oos.writeObject(msg);
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
        Query query1 = new Query(internalInfo.getNumDB()+1, "Select * from utilizador", 1000);
        //dbComm.insertLugares(lugares, query);
        return query1;
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
            //TODO remover o proprio heartbeat
            ansMsg.setServerList(internalInfo.getHeatBeats());
            oos.writeObject(ansMsg);
        } catch (IOException e) {
            System.out.println("[AttendClientThread] - failed to send server list" + e.getMessage());
        }
    }



    private void doRegister(ClientMSG msgClient, DBCommunicationManager dbComm, ObjectOutputStream oos) throws SQLException, IOException {
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

}
