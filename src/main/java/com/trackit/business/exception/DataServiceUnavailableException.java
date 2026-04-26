package com.trackit.business.exception;

public class DataServiceUnavailableException extends RuntimeException {
    public DataServiceUnavailableException(String message) {
        super(message);
    }

    public DataServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
