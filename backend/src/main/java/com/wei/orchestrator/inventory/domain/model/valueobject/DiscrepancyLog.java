package com.wei.orchestrator.inventory.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

public final class DiscrepancyLog {
    private final String sku;
    private final String warehouseId;
    private final int expectedQuantity;
    private final int actualQuantity;
    private final int difference;
    private final LocalDateTime detectedAt;

    private DiscrepancyLog(
            String sku,
            String warehouseId,
            int expectedQuantity,
            int actualQuantity,
            LocalDateTime detectedAt) {
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.expectedQuantity = expectedQuantity;
        this.actualQuantity = actualQuantity;
        this.difference = actualQuantity - expectedQuantity;
        this.detectedAt = detectedAt;
    }

    public static DiscrepancyLog of(
            String sku, String warehouseId, int expectedQuantity, int actualQuantity) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        if (expectedQuantity < 0) {
            throw new IllegalArgumentException("Expected quantity cannot be negative");
        }
        if (actualQuantity < 0) {
            throw new IllegalArgumentException("Actual quantity cannot be negative");
        }
        return new DiscrepancyLog(
                sku.trim(),
                warehouseId.trim(),
                expectedQuantity,
                actualQuantity,
                LocalDateTime.now());
    }

    public String getSku() {
        return sku;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getExpectedQuantity() {
        return expectedQuantity;
    }

    public int getActualQuantity() {
        return actualQuantity;
    }

    public int getDifference() {
        return difference;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public boolean hasDiscrepancy() {
        return difference != 0;
    }

    public boolean isOverstock() {
        return difference > 0;
    }

    public boolean isUnderstock() {
        return difference < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscrepancyLog that = (DiscrepancyLog) o;
        return expectedQuantity == that.expectedQuantity
                && actualQuantity == that.actualQuantity
                && difference == that.difference
                && Objects.equals(sku, that.sku)
                && Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(detectedAt, that.detectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                sku, warehouseId, expectedQuantity, actualQuantity, difference, detectedAt);
    }

    @Override
    public String toString() {
        return "DiscrepancyLog{"
                + "sku='"
                + sku
                + '\''
                + ", warehouseId='"
                + warehouseId
                + '\''
                + ", expectedQuantity="
                + expectedQuantity
                + ", actualQuantity="
                + actualQuantity
                + ", difference="
                + difference
                + ", detectedAt="
                + detectedAt
                + '}';
    }
}
