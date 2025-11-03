package com.wei.orchestrator.wes.query.dto;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.time.LocalDateTime;

public class PickingTaskSummaryDto {

    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final TaskOrigin origin;
    private final int priority;
    private final TaskStatus status;
    private final int itemCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    public PickingTaskSummaryDto(
            String taskId,
            String wesTaskId,
            String orderId,
            TaskOrigin origin,
            int priority,
            TaskStatus status,
            int itemCount,
            LocalDateTime createdAt,
            LocalDateTime completedAt) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.origin = origin;
        this.priority = priority;
        this.status = status;
        this.itemCount = itemCount;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
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

    public int getPriority() {
        return priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getItemCount() {
        return itemCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
