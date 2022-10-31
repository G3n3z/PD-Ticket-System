package com.isec.pd22.server;

import com.isec.pd22.server.models.InternalInfo;
import com.isec.pd22.server.threads.StartServices;

import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Para arrancar o servidor deve passar o porto e a diretoria para uma base de dados");
            System.exit(-1);
        }

        Scanner scanner = new Scanner(System.in);
        String input;
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
        startServer();
        StartServices startServices = new StartServices(info);
        startServices.start();

        while (!isFinish){
            System.out.println("Digite comando: ");
            input = scanner.nextLine();
            if(input.equalsIgnoreCase("exit")) {
                isFinish = true;
                synchronized (info){
                    info.setFinish(true);
                }

            }else {
                synchronized (info){
                    isFinish = info.isFinish();
                }
            }
        }


        //TODO:  Esperar pelas threads terminem, gravar estados
        System.out.println("Adeus e obrigado");

    }

    private static void startServer() {
        //Testar se existe base de dados
        //30s espera por um heartbit

    }
}
