package com.wei.orchestrator.wes.domain.event;

import java.time.LocalDateTime;

public final class PickingTaskCanceledEvent {
    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final String reason;
    private final LocalDateTime occurredAt;

    public PickingTaskCanceledEvent(
            String taskId,
            String wesTaskId,
            String orderId,
            String reason,
            LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
