package com.wei.orchestrator.wes.domain.exception;

import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;

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
