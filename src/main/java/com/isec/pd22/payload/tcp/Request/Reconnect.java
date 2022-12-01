package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.ClientMSG;

public class Reconnect extends ClientMSG {
    private ClientMSG lastSubscription;

    public Reconnect(ClientActions action) {
        super(action);
    }

    public Reconnect() {
    }

    public ClientMSG getSubscription() {
        return lastSubscription;
    }

    public void setSubscription(ClientMSG lastMessageReceive) {
        lastSubscription = lastMessageReceive;
    }
}
