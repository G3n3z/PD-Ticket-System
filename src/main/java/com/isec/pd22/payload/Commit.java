package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class Commit extends MulticastMSG{

    public Commit(TypeOfMulticastMsg typeMsg) {
        super(typeMsg);
    }
}
