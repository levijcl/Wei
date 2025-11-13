package com.wei.orchestrator.observation.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

public class StockSnapshot {
    private final String sku;
    private final int quantity;
    private final String warehouseId;
    private final LocalDateTime timestamp;

    public StockSnapshot(String sku, int quantity, String warehouseId, LocalDateTime timestamp) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.sku = sku;
        this.quantity = quantity;
        this.warehouseId = warehouseId;
        this.timestamp = timestamp;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockSnapshot that = (StockSnapshot) o;
        return quantity == that.quantity
                && Objects.equals(sku, that.sku)
                && Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, quantity, warehouseId, timestamp);
    }

    @Override
    public String toString() {
        return "StockSnapshot{"
                + "sku='"
                + sku
                + '\''
                + ", quantity="
                + quantity
                + ", warehouseId='"
                + warehouseId
                + '\''
                + ", timestamp="
                + timestamp
                + '}';
    }
}
