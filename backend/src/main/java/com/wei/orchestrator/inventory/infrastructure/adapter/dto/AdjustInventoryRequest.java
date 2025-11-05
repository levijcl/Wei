package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdjustInventoryRequest {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("adjustment")
    private Integer adjustment;

    @JsonProperty("reason")
    private String reason;

    public AdjustInventoryRequest() {}

    public AdjustInventoryRequest(
            String sku, String warehouseId, Integer adjustment, String reason) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.adjustment = adjustment;
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

    public Integer getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(Integer adjustment) {
        this.adjustment = adjustment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
