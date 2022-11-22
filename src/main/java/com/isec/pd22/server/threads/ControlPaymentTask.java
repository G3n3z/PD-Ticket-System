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
        dbComm = new DBCommunicationManager(internalInfo, connection);
        while (true) {
            if (dbComm.canCancelReservation(reservationId)) {
                Query query = dbComm.deleteReservaNotPayed(reservationId);
                if (startUpdateRoutine(query, internalInfo)) {
                    try {
                        dbVM.insertQuery(query);
                        synchronized (internalInfo){
                            internalInfo.setNumDB(internalInfo.getNumDB()+1);
                        }
                        sendCommit();
                    } catch (SQLException e) {
                        System.out.println("[ControlPayment] - error on updateRoutine - could not send prepare: " + e.getMessage());
                    } catch (IOException e) {
                        System.out.println("[ControlPayment] - error on updateRoutine - could not send prepare: " + e.getMessage());
                    }finally {
                        break;
                    }
                } else {
                    try {
                        System.out.println("[ControlPaymentTask] - servers busy, trying again in a second...");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } else {
                break;
            }
        }
        cancel();
        timer.cancel();
    }

    private boolean startUpdateRoutine(Query query, InternalInfo internalInfo){
        Set<Prepare> confirmationList = new HashSet<>();
        byte[] bytes = new byte[10000];
        //Mudar estado para UPDATE
        synchronized (internalInfo) {
            internalInfo.setStatus(Status.UPDATING);
        }
        //Abrir socket UPD auto para confirmações
        try {
            DatagramPacket dp = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(Constants.MULTICAST_IP), Constants.MULTICAST_PORT);
            DatagramSocket confirmationSocket = new DatagramSocket(0);
            sendPrepare(dp, query, confirmationList, confirmationSocket);

            return verifyConfirmations(confirmationList);
            //Verificar confirmações com lista de servidores
        } catch (IOException e) {
            System.out.println("[ControlPayment] - error on updateRoutine - could not send prepare: " + e.getMessage());
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
                    if (confirmationList.size() == internalInfo.getHeatBeats().size() && verifyConfirmations(confirmationList)) {
                        repeat = false;
                        break;
                    }
                }
            } catch (SocketException | SocketTimeoutException e) {
                //Verificar confirmações com lista de servidores
                if (confirmationCounter == 0) {
                    break;
                } else if (verifyConfirmations(confirmationList))
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
        objectStream.writeObject(dp, abortMsg);
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
        os.writeObject(dp, commitMsg);
        internalInfo.getMulticastSocket().send(dp);
    }

    private boolean verifyConfirmations(Set<Prepare> confirmationList) {
        synchronized (internalInfo.getHeatBeats()) {
            if (confirmationList.size() == internalInfo.getHeatBeats().size()) {
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
}