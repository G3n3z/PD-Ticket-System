package com.isec.pd22.payload;

import com.isec.pd22.server.models.Query;

public class Prepare extends MulticastMSG{
    Query query;
    int numVersion;

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public int getNumVersion() {
        return numVersion;
    }

    public void setNumVersion(int numVersion) {
        this.numVersion = numVersion;
    }
}
