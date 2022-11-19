package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.ClientMSG;

public class FileUpload extends ClientMSG {

    boolean isLast = false;
    byte [] bytes;
    int sizeBytes;

    String name;


    public FileUpload(ClientActions action) {
        super(action);
    }

    public FileUpload(ClientActions action, boolean isLast, byte[] bytes, int sizeBytes, String name) {
        super(action);
        this.isLast = isLast;
        this.bytes = bytes;
        this.sizeBytes = sizeBytes;
        this.name = name;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(int sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
