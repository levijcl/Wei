package com.wei.orchestrator.wes.domain.exception;

public class WesTimeoutException extends WesOperationException {

    public WesTimeoutException(String message) {
        super(message);
    }

    public WesTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
