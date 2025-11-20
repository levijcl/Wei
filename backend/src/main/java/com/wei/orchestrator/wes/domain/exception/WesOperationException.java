package com.wei.orchestrator.wes.domain.exception;

public class WesOperationException extends RuntimeException {

    public WesOperationException(String message) {
        super(message);
    }

    public WesOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
