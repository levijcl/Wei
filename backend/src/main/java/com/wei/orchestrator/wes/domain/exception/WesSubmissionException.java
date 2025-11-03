package com.wei.orchestrator.wes.domain.exception;

/**
 * Exception thrown when task submission to WES fails.
 * This includes:
 * - WES endpoint not available (404)
 * - WES server errors (5xx)
 * - Network connectivity issues
 * - Invalid request data
 */
public class WesSubmissionException extends WesOperationException {

    public WesSubmissionException(String message) {
        super(message);
    }

    public WesSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
