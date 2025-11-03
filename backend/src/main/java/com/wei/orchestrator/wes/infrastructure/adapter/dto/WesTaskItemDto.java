package com.wei.orchestrator.wes.infrastructure.adapter.dto;

public class WesTaskItemDto {
    private String sku;
    private String productName;
    private int quantity;
    private String location;

    public WesTaskItemDto() {}

    public WesTaskItemDto(String sku, String productName, int quantity, String location) {
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.location = location;
    }

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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
