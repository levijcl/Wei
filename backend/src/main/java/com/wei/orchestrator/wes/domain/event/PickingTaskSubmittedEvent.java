package com.wei.orchestrator.wes.domain.event;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import java.time.LocalDateTime;

public final class PickingTaskSubmittedEvent {
    private final String taskId;
    private final String wesTaskId;
    private final TaskOrigin origin;
    private final LocalDateTime occurredAt;

    public PickingTaskSubmittedEvent(
            String taskId, String wesTaskId, TaskOrigin origin, LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.origin = origin;
        this.occurredAt = occurredAt;
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

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
