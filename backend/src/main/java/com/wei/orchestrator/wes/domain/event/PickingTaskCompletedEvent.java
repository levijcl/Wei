package com.wei.orchestrator.wes.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PickingTaskCompletedEvent implements DomainEvent {
    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public PickingTaskCompletedEvent(
            String taskId, String wesTaskId, String orderId, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.occurredAt = occurredAt;
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public PickingTaskCompletedEvent(
            String taskId,
            String wesTaskId,
            String orderId,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.occurredAt = occurredAt;
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
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

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public TriggerContext getTriggerContext() {
        return triggerContext;
    }
}
