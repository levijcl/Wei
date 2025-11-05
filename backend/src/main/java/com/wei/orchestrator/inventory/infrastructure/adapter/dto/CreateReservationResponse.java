package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationResponse {

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("status")
    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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
}
