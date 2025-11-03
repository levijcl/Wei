package com.wei.orchestrator.wes.domain.model.valueobject;

public enum TaskOrigin {
    ORCHESTRATOR_SUBMITTED,
    WES_DIRECT;

    public boolean isOrchestratorSubmitted() {
        return this == ORCHESTRATOR_SUBMITTED;
    }

    public boolean isWesDirect() {
        return this == WES_DIRECT;
    }
}
