package com.isec.pd22.server.threads;

import com.isec.pd22.enums.Status;
import com.isec.pd22.enums.TypeOfMulticastMsg;
import com.isec.pd22.payload.Abort;
import com.isec.pd22.payload.Commit;
import com.isec.pd22.payload.HeartBeat;
import com.isec.pd22.payload.Prepare;
import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.models.Query;
import com.isec.pd22.utils.Constants;
import com.isec.pd22.utils.DBCommunicationManager;
import com.isec.pd22.utils.DBVersionManager;
import com.isec.pd22.utils.ObjectStream;

import java.io.IOException;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ControlPaymentTask extends TimerTask {

    private int reservationId;
    private Connection connection;
    private InternalInfo internalInfo;
    private DBCommunicationManager dbComm;
    private DBVersionManager dbVM;
    Timer timer;

    public ControlPaymentTask(int reservationId, Connection connection, InternalInfo internalInfo, Timer timer) {
        this.reservationId = reservationId;
        this.connection = connection;
        this.internalInfo = internalInfo;
        this.timer = timer;
    }

    @Override
    public void run() {
        dbVM = new DBVersionManager(connection);
        dbComm = new DBCommunicationManager(internalInfo,connection);
        while(true) {
            System.out.println(dbComm.canCancelReservation(reservationId));
            if (dbComm.canCancelReservation(reservationId)) {
                Query query = dbComm.deleteReservaNotPayed(reservationId);
                if (startUpdateRoutine(query, internalInfo)) {
                    try {
                        dbVM.insertQuery(query);
                        System.out.println("inseri");
                        break;
                    } catch (SQLException e) {
                        System.out.println("[ControlPaymentTask] - failed to insert query: " + e.getMessage());
                    }
                } else{
                    try {
                        System.out.println("[ControlPaymentTask] - servers busy, trying again in a second...");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }else {
                break;
            }
        }
        cancel();
        timer.cancel();
    }

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
                    System.out.println("[ControlPaymentTask] - error on updateRoutine - could not send commit: "+ e.getMessage());
                    return false;
                }
                //Executar alteração
                return true;
            }
            //Se não estiverem todas enviar ABORT
            sendAbort(dp);
            //Cancelar a operação e informar utilizador

        }  catch (IOException e) {
            System.out.println("[ControlPaymentTask] - error on updateRoutine - could not send prepare: "+ e.getMessage());
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

}
