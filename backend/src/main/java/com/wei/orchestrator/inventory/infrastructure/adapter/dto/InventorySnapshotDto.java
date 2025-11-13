package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InventorySnapshotDto {

    @JsonProperty("SKU")
    private String sku;

    @JsonProperty("PRODUCT_NAME")
    private String productName;

    @JsonProperty("WAREHOUSE_ID")
    private String warehouseId;

    @JsonProperty("TOTAL_QUANTITY")
    private Integer totalQuantity;

    @JsonProperty("RESERVED_QUANTITY")
    private Integer reservedQuantity;

    @JsonProperty("AVAILABLE_QUANTITY")
    private Integer availableQuantity;

    @JsonProperty("LOCATION")
    private String location;

    @JsonProperty("UPDATED_AT")
    private String updatedAt;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
