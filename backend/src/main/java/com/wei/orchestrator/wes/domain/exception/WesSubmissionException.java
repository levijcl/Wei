package com.wei.orchestrator.wes.domain.exception;

public class WesSubmissionException extends WesOperationException {

    public WesSubmissionException(String message) {
        super(message);
    }

    public WesSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
