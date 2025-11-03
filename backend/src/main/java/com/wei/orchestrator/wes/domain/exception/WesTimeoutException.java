package com.wei.orchestrator.wes.domain.exception;

/**
 * Exception thrown when communication with WES times out.
 * This includes:
 * - Connection timeout
 * - Read timeout
 * - Network unavailability
 * - DNS resolution failures
 */
public class WesTimeoutException extends WesOperationException {

    public WesTimeoutException(String message) {
        super(message);
    }

    public WesTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
