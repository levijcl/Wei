package com.wei.orchestrator.wes.domain.exception;

/**
 * Base exception for all WES-related operations.
 * Extends RuntimeException to trigger @Transactional rollback.
 */
public class WesOperationException extends RuntimeException {

    public WesOperationException(String message) {
        super(message);
    }

    public WesOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
