package com.isec.pd22.server.models;

public class Query {
    int numVersion;
    String query;
    int timestamp;

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

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
