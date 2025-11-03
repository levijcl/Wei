package com.wei.orchestrator.wes.query.dto;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public class PickingTaskDetailDto {

    private final String taskId;
    private final String wesTaskId;
    private final String orderId;
    private final TaskOrigin origin;
    private final int priority;
    private final TaskStatus status;
    private final List<TaskItemDto> items;
    private final LocalDateTime createdAt;
    private final LocalDateTime submittedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime canceledAt;
    private final String failureReason;

    public PickingTaskDetailDto(
            String taskId,
            String wesTaskId,
            String orderId,
            TaskOrigin origin,
            int priority,
            TaskStatus status,
            List<TaskItemDto> items,
            LocalDateTime createdAt,
            LocalDateTime submittedAt,
            LocalDateTime completedAt,
            LocalDateTime canceledAt,
            String failureReason) {
        this.taskId = taskId;
        this.wesTaskId = wesTaskId;
        this.orderId = orderId;
        this.origin = origin;
        this.priority = priority;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.completedAt = completedAt;
        this.canceledAt = canceledAt;
        this.failureReason = failureReason;
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

    public List<TaskItemDto> getItems() {
        return items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public static class TaskItemDto {
        private final String sku;
        private final int quantity;
        private final String location;

        public TaskItemDto(String sku, int quantity, String location) {
            this.sku = sku;
            this.quantity = quantity;
            this.location = location;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getLocation() {
            return location;
        }
    }
}
