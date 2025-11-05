package com.wei.orchestrator.inventory.application.command;

public class ReserveInventoryCommand {
    private final String orderId;
    private final String sku;
    private final String warehouseId;
    private final int quantity;

    public ReserveInventoryCommand(String orderId, String sku, String warehouseId, int quantity) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.orderId = orderId;
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }
}
