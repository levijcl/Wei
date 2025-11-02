package com.wei.orchestrator.wes.domain.event;

import java.time.LocalDateTime;

public final class PickingTaskSubmittedEvent {
    private final String taskId;
    private final String wesTaskId;
    private final LocalDateTime occurredAt;

    public PickingTaskSubmittedEvent(String taskId, String wesTaskId, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.occurredAt = occurredAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getWesTaskId() {
        return wesTaskId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
