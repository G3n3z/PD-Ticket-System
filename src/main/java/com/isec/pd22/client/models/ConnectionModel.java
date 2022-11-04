package models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ConnectionModel {
    private static long id = 0;

    private final Socket socket;

    private final ObjectOutputStream objOutputStream;

    private final ObjectInputStream objInputStream;

    public ConnectionModel(Socket socket) throws IOException {
        ConnectionModel.id += 1;

        this.socket = socket;

        this.objOutputStream = new ObjectOutputStream(socket.getOutputStream());
        this.objInputStream =  new ObjectInputStream(socket.getInputStream());
    }

    public long getId() {
        return id;
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getObjOutputStream() {
        return objOutputStream;
    }

    public ObjectInputStream getObjInputStream() {
        return objInputStream;
    }
}
