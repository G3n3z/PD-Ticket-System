package com.isec.pd22.server;

import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        if(args.length != 2){
            System.out.println("Para arrancar o servidor deve passar o porto e a diretoria para uma base de dados");
        }

        Scanner scanner = new Scanner(System.in);
        String input;
        try{
            int port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e){
            System.out.println("Indique um porto numerico");
            System.exit(-1);
        }

        String url_database = args[1];
        startServer();

        while (true){
            input = scanner.nextLine();
            if(input.equalsIgnoreCase("exit")) {
                break;
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
