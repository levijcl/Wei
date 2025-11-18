package com.wei.orchestrator.wes.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import java.time.LocalDateTime;
import java.util.UUID;

public final class PickingTaskSubmittedEvent implements DomainEvent {
    private final String taskId;
    private final String wesTaskId;
    private final TaskOrigin origin;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public PickingTaskSubmittedEvent(
            String taskId, String wesTaskId, TaskOrigin origin, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.origin = origin;
        this.occurredAt = occurredAt;
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public PickingTaskSubmittedEvent(
            String taskId,
            String wesTaskId,
            TaskOrigin origin,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.origin = origin;
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

    public TaskOrigin getOrigin() {
        return origin;
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
