package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationRequest {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("quantity")
    private Integer quantity;

    public CreateReservationRequest() {}

    public CreateReservationRequest(
            String sku, String warehouseId, String orderId, Integer quantity) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.orderId = orderId;
        this.quantity = quantity;
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
}
