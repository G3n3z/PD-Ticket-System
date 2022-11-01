package com.isec.pd22.server.models;

import java.io.Serializable;

public class Query implements Serializable {
    int numVersion;
    String query;
    long timestamp;

    public Query() {
    }

    public Query(int numVersion, String query, int timestamp) {
        this.numVersion = numVersion;
        this.query = query;
        this.timestamp = timestamp;
    }

    public int getNumVersion() {
        return numVersion;
    }

    public void setNumVersion(int numVersion) {
        this.numVersion = numVersion;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
