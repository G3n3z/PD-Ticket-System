package com.isec.pd22.payload;

import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.enums.Status;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ServersRequestPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    public static final int MAX_PAYLOAD_BYTES = 10000;

    private List<HeartBeat> serversListCollection = null;

    private final ClientsPayloadType clientsPayloadType;

    public ServersRequestPayload(ArrayList<HeartBeat> serversList, ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
        this.serversListCollection = serversList.stream().filter(heartBeat -> heartBeat.statusServer != Status.UNAVAILABLE).collect(Collectors.<HeartBeat>toList());
    }

    public ServersRequestPayload(ClientsPayloadType clientsPayloadType) {
        this.clientsPayloadType = clientsPayloadType;
    }

    public List<HeartBeat> getServersListCollection() { return serversListCollection; }

    public ClientsPayloadType getClientsPayloadType() { return clientsPayloadType; }
}
