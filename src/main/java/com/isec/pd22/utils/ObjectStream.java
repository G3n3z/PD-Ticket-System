package com.isec.pd22.utils;

import java.io.*;
import java.net.DatagramPacket;

public class ObjectStream {
    ObjectOutputStream oos;
    ByteArrayOutputStream baos;
    public <T>  ByteArrayOutputStream getByteArrayOutputStreamByClass(T object){
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(object);
            return  baos;
        }catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    public <T> boolean writeObject(DatagramPacket packet, T object) throws IOException {
        ByteArrayOutputStream baos = getByteArrayOutputStreamByClass(object);
        if(baos == null){
            return false;
        }
        packet.setData(baos.toByteArray());
        packet.setLength(baos.size());
        oos.close();
        baos.close();


        return true;
    }

    public <T> T readObject(DatagramPacket packet, Class<T> tClass) throws IOException  {
        ObjectInputStream in;
        try{
            in =  new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        }catch (IOException e){
            return null;
        }
        Object object = null;
        T obj = null;
        try {
            object = in.readObject();
            if(object != null && tClass.isAssignableFrom(object.getClass())){
                obj = (T) object;
            }

        } catch ( ClassNotFoundException e) {
            System.out.println(e);
            return null;
        }

        return obj;
    }

}
