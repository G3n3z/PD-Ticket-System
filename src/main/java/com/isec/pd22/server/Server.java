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
        if(args.length != 2){
            System.out.println("Para arrancar o servidor deve passar o porto e a diretoria para uma base de dados");
            System.exit(-1);
        }

        Scanner scanner = new Scanner(System.in);
        int input;
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
            MulticastSocket multicastSocket =  connectMulticastGroup();
            info.setMulticastSocket(multicastSocket);
            info.setIp(InetAddress.getLocalHost().getHostAddress());
            System.out.println(info.getIp());
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
            input = scanner.nextInt();

            switch (input){
                case 1 -> {
                    isFinish = true;
                    finish(info);
                }
                case 2 -> {
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
        //TODO:  Esperar pelas threads terminem, gravar estados
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
        info.getMulticastSocket().close();
    }

    private static void imprimeMenu() {
        System.out.println("******************");
        System.out.println("       Menu       ");
        System.out.println("1 - Exit");
        System.out.println("2 - Listar Servidores");
    }

    private static void startServer() {
        //Testar se existe base de dados
        //30s espera por um heartbit

    }

    private static MulticastSocket connectMulticastGroup() throws IOException {

        MulticastSocket socket = new MulticastSocket(Constants.MULTICAST_PORT);
        InetAddress group = InetAddress.getByName(Constants.MULTICAST_IP);
        SocketAddress sa = new InetSocketAddress(group, Constants.MULTICAST_PORT);
        //NetworkInterface nif = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        NetworkInterface nif = NetworkInterface.getByName("en0");
        socket.joinGroup(sa, nif);
        return socket;
    }

}
