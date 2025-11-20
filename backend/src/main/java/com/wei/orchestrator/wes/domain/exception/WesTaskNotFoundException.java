package com.wei.orchestrator.wes.domain.exception;

import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;

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
