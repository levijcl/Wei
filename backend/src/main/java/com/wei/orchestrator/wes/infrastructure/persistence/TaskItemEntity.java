package com.wei.orchestrator.wes.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "task_items")
public class TaskItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    public TaskItemEntity() {}

    public TaskItemEntity(String taskId, String sku, Integer quantity, String location) {
        this.taskId = taskId;
        this.sku = sku;
        this.quantity = quantity;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
