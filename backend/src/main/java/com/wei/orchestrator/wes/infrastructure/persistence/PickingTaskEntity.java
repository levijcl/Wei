package com.wei.orchestrator.wes.infrastructure.persistence;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "picking_tasks")
public class PickingTaskEntity {
    @Id
    @Column(name = "task_id", length = 100)
    private String taskId;

    @Column(name = "wes_task_id", length = 100)
    private String wesTaskId;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 50)
    private TaskOrigin origin;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public PickingTaskEntity() {}

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWesTaskId() {
        return wesTaskId;
    }

    public void setWesTaskId(String wesTaskId) {
        this.wesTaskId = wesTaskId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public TaskOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(TaskOrigin origin) {
        this.origin = origin;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
