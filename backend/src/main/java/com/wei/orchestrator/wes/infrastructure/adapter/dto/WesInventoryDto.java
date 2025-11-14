package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WesInventoryDto {

    @JsonProperty("SKU")
    private String sku;

    @JsonProperty("PRODUCT_NAME")
    private String productName;

    @JsonProperty("WAREHOUSE_ID")
    private String warehouseId;

    @JsonProperty("QUANTITY")
    private Integer quantity;

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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
