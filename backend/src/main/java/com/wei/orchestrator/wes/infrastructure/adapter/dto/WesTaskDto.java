package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WesTaskDto {
    @JsonProperty("TASK_ID")
    private String taskId;

    @JsonProperty("TASK_TYPE")
    private String taskType;

    @JsonProperty("ORDER_ID")
    private String orderId;

    @JsonProperty("WAREHOUSE_ID")
    private String warehouseId;

    @JsonProperty("PRIORITY")
    private Integer priority;

    @JsonProperty("STATUS")
    private String status;

    @JsonProperty("CREATED_AT")
    private String createdAt;

    @JsonProperty("STARTED_AT")
    private String startedAt;

    @JsonProperty("COMPLETED_AT")
    private String completedAt;

    public WesTaskDto() {}

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
