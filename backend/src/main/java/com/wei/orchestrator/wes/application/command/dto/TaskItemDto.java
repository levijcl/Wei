package com.wei.orchestrator.wes.application.command.dto;

public class TaskItemDto {

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
