package com.wei.orchestrator.wes.domain.exception;

import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;

/**
 * Exception thrown when updating task priority in WES fails.
 * This includes:
 * - WES server errors during priority update (5xx)
 * - Invalid priority values rejected by WES
 * - Task in a state that doesn't allow priority updates
 */
public class WesPriorityUpdateException extends WesOperationException {

    private final WesTaskId wesTaskId;
    private final Integer attemptedPriority;

    public WesPriorityUpdateException(WesTaskId wesTaskId, int attemptedPriority, String message) {
        super(message);
        this.wesTaskId = wesTaskId;
        this.attemptedPriority = attemptedPriority;
    }

    public WesPriorityUpdateException(
            WesTaskId wesTaskId, int attemptedPriority, String message, Throwable cause) {
        super(message, cause);
        this.wesTaskId = wesTaskId;
        this.attemptedPriority = attemptedPriority;
    }

    public WesTaskId getWesTaskId() {
        return wesTaskId;
    }

    public Integer getAttemptedPriority() {
        return attemptedPriority;
    }
}
