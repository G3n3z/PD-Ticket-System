package com.isec.pd22.payload;

import java.util.ArrayList;
import java.util.Collection;

public class ClientConnectionPayload {
    public static final int MAX_PAYLOAD_BYTES = 4000;

    private Collection<?> serversListCollection;

    public ClientConnectionPayload(ArrayList<?> serversList) {
        this.serversListCollection = serversList.stream().filter();
    }
}
