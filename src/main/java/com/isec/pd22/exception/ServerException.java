package com.isec.pd22.exception;

public class ServerException extends RuntimeException {

    String message;

    public ServerException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
