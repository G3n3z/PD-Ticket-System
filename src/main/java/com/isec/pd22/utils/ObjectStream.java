package com.isec.pd22.utils;

import java.io.*;
import java.net.DatagramPacket;

public class ObjectStream {

    public <T>  ByteArrayOutputStream getByteArrayOutputStreamByClass(T object){
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            return  baos;
        }catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    public <T> boolean writeObject(DatagramPacket packet, T object){
        ByteArrayOutputStream baos = getByteArrayOutputStreamByClass(object);
        if(baos == null){
            return false;
        }
        packet.setData(baos.toByteArray());
        packet.setLength(baos.size());
        return true;
    }

    public <T> T readObject(DatagramPacket packet, Class<T> tClass) {
        ObjectInputStream in;
        try{
            in =  new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        }catch (IOException e){
            return null;
        }
        Object object = null;
        T obj = null;
        try {
            object =  in.readObject();
            if(object != null && tClass.isAssignableFrom(object.getClass())){
                obj = (T) object;
            }

        } catch (IOException | ClassNotFoundException e) {
            return null;
        }

        return obj;
    }

}
