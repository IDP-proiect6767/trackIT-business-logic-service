package com.trackit.business.exception;

public class DataServiceClientException extends RuntimeException {
    private final int statusCode;

    public DataServiceClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
