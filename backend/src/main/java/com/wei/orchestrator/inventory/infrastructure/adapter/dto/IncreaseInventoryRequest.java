package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IncreaseInventoryRequest {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("reason")
    private String reason;

    public IncreaseInventoryRequest() {}

    public IncreaseInventoryRequest(
            String sku, String warehouseId, Integer quantity, String reason) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.reason = reason;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
