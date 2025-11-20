package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class WesTaskStatusUpdatedEvent implements DomainEvent {
    private final String taskId;
    private final TaskStatus newStatus;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public WesTaskStatusUpdatedEvent(String taskId, TaskStatus newStatus) {
        this.taskId = taskId;
        this.newStatus = newStatus;
        this.occurredAt = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public WesTaskStatusUpdatedEvent(
            String taskId, TaskStatus newStatus, TriggerContext triggerContext) {
        this.taskId = taskId;
        this.newStatus = newStatus;
        this.occurredAt = LocalDateTime.now();
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
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

    @Override
    public String toString() {
        return "WesTaskStatusUpdatedEvent{"
                + "taskId='"
                + taskId
                + '\''
                + ", newStatus="
                + newStatus
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
