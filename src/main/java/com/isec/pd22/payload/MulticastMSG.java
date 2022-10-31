package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class MulticastMSG {

    int versionDB;
    TypeOfMulticastMsg typeMsg;


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

