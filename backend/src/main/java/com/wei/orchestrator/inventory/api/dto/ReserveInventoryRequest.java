package com.wei.orchestrator.inventory.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ReserveInventoryRequest {

    @NotBlank(message = "Order ID is required") private String orderId;

    @NotBlank(message = "SKU is required") private String sku;

    @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") private Integer quantity;

    public ReserveInventoryRequest() {}

    public ReserveInventoryRequest(String orderId, String sku, Integer quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
}
