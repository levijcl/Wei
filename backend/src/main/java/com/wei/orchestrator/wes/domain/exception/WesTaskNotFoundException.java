package com.wei.orchestrator.wes.domain.exception;

import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;

/**
 * Exception thrown when a WES task is not found (HTTP 404).
 * This occurs when:
 * - Querying status of a non-existent task
 * - Attempting to update priority of a deleted task
 * - Attempting to cancel a task that doesn't exist
 */
public class WesTaskNotFoundException extends WesOperationException {

    private final WesTaskId wesTaskId;

    public WesTaskNotFoundException(WesTaskId wesTaskId) {
        super("WES task not found: " + wesTaskId.getValue());
        this.wesTaskId = wesTaskId;
    }

    public WesTaskNotFoundException(WesTaskId wesTaskId, Throwable cause) {
        super("WES task not found: " + wesTaskId.getValue(), cause);
        this.wesTaskId = wesTaskId;
    }

    public WesTaskId getWesTaskId() {
        return wesTaskId;
    }
}
