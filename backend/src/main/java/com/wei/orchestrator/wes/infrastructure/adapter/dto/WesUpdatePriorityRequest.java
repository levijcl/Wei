package com.wei.orchestrator.wes.infrastructure.adapter.dto;

public class WesUpdatePriorityRequest {
    private int priority;

    public WesUpdatePriorityRequest() {}

    public WesUpdatePriorityRequest(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
