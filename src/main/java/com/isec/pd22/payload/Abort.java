package com.isec.pd22.payload;

import com.isec.pd22.enums.TypeOfMulticastMsg;

public class Abort extends MulticastMSG{

    public Abort(TypeOfMulticastMsg typeMsg) {
        super(typeMsg);
    }
}
