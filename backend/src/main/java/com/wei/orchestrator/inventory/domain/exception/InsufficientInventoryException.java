package com.wei.orchestrator.inventory.domain.exception;

public class InsufficientInventoryException extends RuntimeException {
    private final String sku;
    private final String warehouseId;
    private final int requested;
    private final int available;

    public InsufficientInventoryException(
            String sku, String warehouseId, int requested, int available) {
        super(
                String.format(
                        "Insufficient inventory for SKU %s in warehouse %s. Requested: %d,"
                                + " Available: %d",
                        sku, warehouseId, requested, available));
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.requested = requested;
        this.available = available;
    }

    public InsufficientInventoryException(String message) {
        super(message);
        this.sku = null;
        this.warehouseId = null;
        this.requested = 0;
        this.available = 0;
    }

    public String getSku() {
        return sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
