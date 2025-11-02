package com.wei.orchestrator.wes.domain.exception;

import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;

/**
 * Exception thrown when task cancellation in WES fails.
 * This includes:
 * - WES server errors during cancellation (5xx)
 * - Task in a state that cannot be cancelled (e.g., already completed)
 * - WES business rules preventing cancellation
 */
public class WesTaskCancellationException extends WesOperationException {

    private final WesTaskId wesTaskId;

    public WesTaskCancellationException(WesTaskId wesTaskId, String message) {
        super(message);
        this.wesTaskId = wesTaskId;
    }

    public WesTaskCancellationException(WesTaskId wesTaskId, String message, Throwable cause) {
        super(message, cause);
        this.wesTaskId = wesTaskId;
    }

    public WesTaskId getWesTaskId() {
        return wesTaskId;
    }
}
