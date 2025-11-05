package com.wei.orchestrator.inventory.application.command;

public class IncreaseInventoryCommand {
    private final String sku;
    private final String warehouseId;
    private final int quantity;
    private final String reason;
    private final String sourceReferenceId;

    public IncreaseInventoryCommand(
            String sku, String warehouseId, int quantity, String reason, String sourceReferenceId) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }

        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.reason = reason;
        this.sourceReferenceId = sourceReferenceId;
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

    public String getReason() {
        return reason;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }
}
