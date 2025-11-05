package com.wei.orchestrator.inventory.application.command;

public class AdjustInventoryCommand {
    private final String sku;
    private final String warehouseId;
    private final int quantityChange;
    private final String reason;
    private final String sourceReferenceId;

    public AdjustInventoryCommand(
            String sku,
            String warehouseId,
            int quantityChange,
            String reason,
            String sourceReferenceId) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        if (quantityChange == 0) {
            throw new IllegalArgumentException("Quantity change cannot be zero");
        }
        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }

        this.sku = sku;
        this.warehouseId = warehouseId;
        this.quantityChange = quantityChange;
        this.reason = reason;
        this.sourceReferenceId = sourceReferenceId;
    }

    public String getSku() {
        return sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public String getReason() {
        return reason;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }
}
