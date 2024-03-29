package com.isec.pd22.server;


import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.threads.MulticastThread;
import com.isec.pd22.server.threads.StartServices;
import com.isec.pd22.utils.Constants;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        if(args.length != 3){
            System.out.println("Para arrancar o servidor deve passar o porto, a diretoria para uma base de dados e placa de rede");
            System.exit(-1);
        }

        Scanner scanner = new Scanner(System.in);

        int port = 0;
        try{
             port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e){
            System.out.println("Indique um porto numerico");
            System.exit(-1);
        }

        String url_database = args[1];
        Boolean isFinish = false;
        InternalInfo info = new InternalInfo(url_database, port, isFinish);
        try {
            MulticastSocket multicastSocket =  connectMulticastGroup(args[2]);
            info.setMulticastSocket(multicastSocket);

            info.setIp(args[2]);
            System.out.println(args[2]);
            //info.setIp("10.65.132.195");
            //info.setIp("127.0.0.1");
        } catch (IOException e) {
            System.out.println("Não foi possivel criar o socket multicast");
            return;
        }

        StartServices startServices = new StartServices(info);
        startServices.start();

        while (!isFinish){
            imprimeMenu();
            String input = scanner.nextLine();
            input = input.trim();
            switch (input){
                case "1" -> {
                    isFinish = true;
                    finish(info);
                    startServices.close();
                }
                case "2" -> {
                    synchronized (info.getHeatBeats()){
                        info.getHeatBeats().forEach(System.out::println);
                    }
                }
            }

            isFinish = info.isFinish();

        }

        startServices.finishTimer();
        try {
            startServices.join();
        } catch (InterruptedException ignored) {
        }
        System.out.println("Adeus e obrigado");

    }

    private static void finish(InternalInfo info) {
        int count = 0;
        synchronized (info){
            info.setFinish(true);
        }
        synchronized (info.getHeatBeats()){
            count = info.getHeatBeats().size();
        }
        if(count > 1)
            MulticastThread.sendExitMessage(info, info.getMulticastSocket());

    }

    private static void imprimeMenu() {
        System.out.println("******************");
        System.out.println("       Menu       ");
        System.out.println("1 - Exit");
        System.out.println("2 - Listar Servidores");
    }


    private static MulticastSocket connectMulticastGroup(String ip) throws IOException {

        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        InetAddress group = InetAddress.getByName(Constants.MULTICAST_IP);
        SocketAddress sa = new InetSocketAddress(group, Constants.MULTICAST_PORT);
        //NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getByName(ip));

        socket.joinGroup(sa, nif);
        return socket;
    }

}
