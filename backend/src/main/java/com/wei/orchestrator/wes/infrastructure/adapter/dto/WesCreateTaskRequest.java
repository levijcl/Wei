package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WesCreateTaskRequest {
    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("items")
    private List<WesTaskItemDto> items;

    public WesCreateTaskRequest() {}

    public WesCreateTaskRequest(
            String taskType,
            String orderId,
            String warehouseId,
            int priority,
            List<WesTaskItemDto> items) {
        this.taskType = taskType;
        this.orderId = orderId;
        this.warehouseId = warehouseId;
        this.priority = priority;
        this.items = items;
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

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public List<WesTaskItemDto> getItems() {
        return items;
    }

    public void setItems(List<WesTaskItemDto> items) {
        this.items = items;
    }
}
