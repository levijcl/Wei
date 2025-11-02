package com.wei.orchestrator.wes.domain.model;

import com.wei.orchestrator.wes.domain.event.PickingTaskCanceledEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskCompletedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskCreatedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskFailedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskPriorityAdjustedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskSubmittedEvent;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PickingTask {
    private String taskId;
    private WesTaskId wesTaskId;
    private String orderId;
    private TaskOrigin origin;
    private int priority;
    private TaskStatus status;
    private List<TaskItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private String failureReason;

    private List<Object> domainEvents = new ArrayList<>();

    public PickingTask() {}

    public static PickingTask createForOrder(String orderId, List<TaskItem> items, int priority) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Task items cannot be null or empty");
        }
        validatePriority(priority);

        PickingTask task = new PickingTask();
        task.taskId = generateTaskId();
        task.orderId = orderId;
        task.origin = TaskOrigin.ORCHESTRATOR_SUBMITTED;
        task.priority = priority;
        task.status = TaskStatus.PENDING;
        task.items = new ArrayList<>(items);
        task.createdAt = LocalDateTime.now();

        task.addDomainEvent(
                new PickingTaskCreatedEvent(
                        task.taskId,
                        task.orderId,
                        task.origin,
                        task.priority,
                        task.items.stream()
                                .map(
                                        item ->
                                                new PickingTaskCreatedEvent.TaskItemDto(
                                                        item.getSku(),
                                                        item.getQuantity(),
                                                        item.getLocation()))
                                .collect(Collectors.toList()),
                        task.createdAt));

        return task;
    }

    public static PickingTask createFromWesTask(
            String wesTaskIdValue, List<TaskItem> items, int priority) {
        if (wesTaskIdValue == null || wesTaskIdValue.isBlank()) {
            throw new IllegalArgumentException("WES task ID cannot be null or blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Task items cannot be null or empty");
        }
        validatePriority(priority);

        PickingTask task = new PickingTask();
        task.taskId = generateTaskId();
        task.wesTaskId = WesTaskId.of(wesTaskIdValue);
        task.orderId = null;
        task.origin = TaskOrigin.WES_DIRECT;
        task.priority = priority;
        task.status = TaskStatus.SUBMITTED;
        task.items = new ArrayList<>(items);
        task.createdAt = LocalDateTime.now();
        task.submittedAt = LocalDateTime.now();

        task.addDomainEvent(
                new PickingTaskCreatedEvent(
                        task.taskId,
                        null,
                        task.origin,
                        task.priority,
                        task.items.stream()
                                .map(
                                        item ->
                                                new PickingTaskCreatedEvent.TaskItemDto(
                                                        item.getSku(),
                                                        item.getQuantity(),
                                                        item.getLocation()))
                                .collect(Collectors.toList()),
                        task.createdAt));

        return task;
    }

    public void submitToWes(WesTaskId wesTaskId) {
        if (wesTaskId == null) {
            throw new IllegalArgumentException("WES task ID cannot be null");
        }
        if (!status.canSubmit()) {
            throw new IllegalStateException("Task cannot be submitted in status: " + status);
        }

        this.wesTaskId = wesTaskId;
        this.status = TaskStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();

        addDomainEvent(
                new PickingTaskSubmittedEvent(this.taskId, wesTaskId.getValue(), this.submittedAt));
    }

    public void updateStatusFromWes(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        if (!status.canUpdateFromWes()) {
            throw new IllegalStateException(
                    "Cannot update status from WES when in status: " + status);
        }
        if (status == newStatus) {
            return;
        }

        this.status = newStatus;

        if (newStatus == TaskStatus.COMPLETED) {
            this.completedAt = LocalDateTime.now();
            addDomainEvent(
                    new PickingTaskCompletedEvent(
                            this.taskId,
                            this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                            this.orderId,
                            this.completedAt));
        } else if (newStatus == TaskStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
            addDomainEvent(
                    new PickingTaskFailedEvent(
                            this.taskId,
                            this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                            this.orderId,
                            this.failureReason,
                            this.completedAt));
        } else if (newStatus == TaskStatus.CANCELED) {
            this.canceledAt = LocalDateTime.now();
            addDomainEvent(
                    new PickingTaskCanceledEvent(
                            this.taskId,
                            this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                            this.orderId,
                            this.failureReason,
                            this.canceledAt));
        }
    }

    public void adjustPriority(int newPriority) {
        validatePriority(newPriority);

        if (this.priority == newPriority) {
            return;
        }

        int oldPriority = this.priority;
        this.priority = newPriority;

        addDomainEvent(
                new PickingTaskPriorityAdjustedEvent(
                        this.taskId, oldPriority, newPriority, LocalDateTime.now()));
    }

    public void markCompleted() {
        if (status.isTerminal()) {
            throw new IllegalStateException("Task is already in terminal status: " + status);
        }

        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        addDomainEvent(
                new PickingTaskCompletedEvent(
                        this.taskId,
                        this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                        this.orderId,
                        this.completedAt));
    }

    public void markFailed(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Task is already in terminal status: " + status);
        }

        this.status = TaskStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = reason;

        addDomainEvent(
                new PickingTaskFailedEvent(
                        this.taskId,
                        this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                        this.orderId,
                        reason,
                        this.completedAt));
    }

    public void cancel(String reason) {
        if (!status.canCancel()) {
            throw new IllegalStateException("Task cannot be canceled in status: " + status);
        }

        this.status = TaskStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.failureReason = reason;

        addDomainEvent(
                new PickingTaskCanceledEvent(
                        this.taskId,
                        this.wesTaskId != null ? this.wesTaskId.getValue() : null,
                        this.orderId,
                        reason,
                        this.canceledAt));
    }

    private static String generateTaskId() {
        return "PICK-" + UUID.randomUUID().toString();
    }

    private static void validatePriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException(
                    "Priority must be between 1 and 10, got: " + priority);
        }
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    public String getTaskId() {
        return taskId;
    }

    public WesTaskId getWesTaskId() {
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

    public List<TaskItem> getItems() {
        return Collections.unmodifiableList(items);
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

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setWesTaskId(WesTaskId wesTaskId) {
        this.wesTaskId = wesTaskId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setOrigin(TaskOrigin origin) {
        this.origin = origin;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public void setItems(List<TaskItem> items) {
        this.items = items;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
