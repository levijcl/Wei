package com.wei.orchestrator.wes.domain.event;

import java.time.LocalDateTime;

public final class PickingTaskPriorityAdjustedEvent {
    private final String taskId;
    private final int oldPriority;
    private final int newPriority;
    private final LocalDateTime occurredAt;

    public PickingTaskPriorityAdjustedEvent(
            String taskId, int oldPriority, int newPriority, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.oldPriority = oldPriority;
        this.newPriority = newPriority;
        this.occurredAt = occurredAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public int getOldPriority() {
        return oldPriority;
    }

    public int getNewPriority() {
        return newPriority;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
