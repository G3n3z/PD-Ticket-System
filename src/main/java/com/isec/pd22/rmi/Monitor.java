package com.isec.pd22.rmi;

import com.isec.pd22.utils.Constants;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.UUID;

public class Monitor {



    public static void main(String[] args) {
        boolean keepGoing = true;
        if (args.length != 2){
            System.out.println("Please enter ip and port");
            System.exit(-1);
        }
        Integer port = 0;
        MonitorRmiRemote clientRemote = null;
        try {
            port = Integer.parseInt(args[1]);
        }catch (NumberFormatException e){
            System.out.println("2 parametro deve ser um inteiro");
            System.exit(-1);
        }
        try{
            Registry registry = LocateRegistry.getRegistry(args[0], Registry.REGISTRY_PORT);
            ServerRemoteInterface serverRemote = (ServerRemoteInterface)registry.lookup(Constants.SERVER_SERVICE_NAME+port);
            System.out.println("Connected to Remote Service");
            Scanner sc = new Scanner(System.in);
            boolean registeredToCallbacks = false;
            clientRemote = new MonitorRmiRemote();
            String uuid = UUID.randomUUID().toString();
            while (keepGoing) {
                printOptions(registeredToCallbacks);
                String input = sc.nextLine();
                int in = parseIntegerFromString(input);
                switch (in){
                    case 1 -> {
                        if(!registeredToCallbacks) {
                            registeredToCallbacks = serverRemote.registerToNotifications(uuid, clientRemote);
                            if (!registeredToCallbacks)
                                System.out.println("Não foi possivel registar");
                        }
                        else{
                            registeredToCallbacks = serverRemote.unregisterToNotifications(uuid, clientRemote);
                            if (!registeredToCallbacks){
                                System.out.println("Não foi possivel desfazer o registo");
                            }else{
                                registeredToCallbacks = false;
                            }

                        }
                    }
                    case 2 -> {System.out.println(serverRemote.getListOfServers());}
                    case 3 -> {
                        serverRemote.unregisterToNotifications(uuid,clientRemote);
                        keepGoing = false;
                    }
                    default -> System.out.println("Invalid Option");
                }
            }

        }catch (NotBoundException e){
            System.out.println ("O servico nao esta disponivel!");
        }catch (RemoteException e){
            System.out.println ("Erro de acesso ao servico - ");
        }catch (Exception e){
            System.out.println ("Error - " + e);
        }finally {
            try{
                if(clientRemote != null)
                    UnicastRemoteObject.unexportObject(clientRemote, true);
            }catch (Exception ignored){}
        }

        System.out.println("Vamos Terminar");

    }

    private static int parseIntegerFromString(String input) {
        try{
            return Integer.parseInt(input);
        }catch (NumberFormatException | NullPointerException e){
            return -1;
        }
    }

    private static void printOptions(boolean registeredToCallbacks) {
        System.out.println("Menu");
        System.out.println("1 - " + (registeredToCallbacks ? "Unregistering to notifications" : "Registering to notifications"));
        System.out.println("2 - Get List of Servers");
        System.out.println("3 - Exit");
    }
}
