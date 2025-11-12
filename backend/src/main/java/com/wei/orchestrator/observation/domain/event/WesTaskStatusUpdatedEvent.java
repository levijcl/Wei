package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.time.LocalDateTime;

public class WesTaskStatusUpdatedEvent {
    private final String taskId;
    private final TaskStatus newStatus;
    private final LocalDateTime occurredAt;

    public WesTaskStatusUpdatedEvent(String taskId, TaskStatus newStatus) {
        this.taskId = taskId;
        this.newStatus = newStatus;
        this.occurredAt = LocalDateTime.now();
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
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
