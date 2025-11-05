package com.wei.orchestrator.inventory.domain.model.valueobject;

import java.util.Objects;

public final class WarehouseLocation {
    private final String warehouseId;
    private final String zone;

    private WarehouseLocation(String warehouseId, String zone) {
        this.warehouseId = warehouseId;
        this.zone = zone;
    }

    public static WarehouseLocation of(String warehouseId, String zone) {
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        return new WarehouseLocation(warehouseId.trim(), zone != null ? zone.trim() : null);
    }

    public static WarehouseLocation of(String warehouseId) {
        return of(warehouseId, null);
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getZone() {
        return zone;
    }

    public boolean hasZone() {
        return zone != null && !zone.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WarehouseLocation that = (WarehouseLocation) o;
        return Objects.equals(warehouseId, that.warehouseId) && Objects.equals(zone, that.zone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, zone);
    }

    @Override
    public String toString() {
        if (hasZone()) {
            return String.format(
                    "WarehouseLocation{warehouseId='%s', zone='%s'}", warehouseId, zone);
        }
        return String.format("WarehouseLocation{warehouseId='%s'}", warehouseId);
    }
}
