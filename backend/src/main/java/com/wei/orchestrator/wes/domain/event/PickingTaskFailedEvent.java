package com.wei.orchestrator.wes.domain.event;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import java.time.LocalDateTime;

public final class PickingTaskFailedEvent {
    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final TaskOrigin origin;
    private final String reason;
    private final LocalDateTime occurredAt;

    public PickingTaskFailedEvent(
            String taskId,
            String wesTaskId,
            String orderId,
            TaskOrigin origin,
            String reason,
            LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.origin = origin;
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

    public TaskOrigin getOrigin() {
        return origin;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
