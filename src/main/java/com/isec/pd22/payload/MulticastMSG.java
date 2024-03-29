package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

import java.io.Serial;
import java.io.Serializable;

public class MulticastMSG implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    int versionDB;
    TypeOfMulticastMsg typeMsg;

    public MulticastMSG() {
    }

    public MulticastMSG(TypeOfMulticastMsg typeMsg) {
        this.typeMsg = typeMsg;
    }

    public TypeOfMulticastMsg getTypeMsg() {
        return typeMsg;
    }

    public void setTypeMsg(TypeOfMulticastMsg typeMsg) {
        this.typeMsg = typeMsg;
    }

    public int getVersionDB() {
        return versionDB;
    }

    public void setVersionDB(int versionDB) {
        this.versionDB = versionDB;
    }
}

