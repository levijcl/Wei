package com.wei.orchestrator.wes.domain.event;

import java.time.LocalDateTime;

public final class PickingTaskCompletedEvent {
    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final LocalDateTime occurredAt;

    public PickingTaskCompletedEvent(
            String taskId, String wesTaskId, String orderId, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.occurredAt = occurredAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getWesTaskId() {
        return wesTaskId;
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
