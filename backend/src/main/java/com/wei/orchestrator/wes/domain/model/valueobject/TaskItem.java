package com.wei.orchestrator.wes.domain.model.valueobject;

import java.util.Objects;

public final class TaskItem {
    private final String sku;
    private final int quantity;
    private final String location;

    private TaskItem(String sku, int quantity, String location) {
        this.sku = sku;
        this.quantity = quantity;
        this.location = location;
    }

    public static TaskItem of(String sku, int quantity, String location) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Location cannot be null or blank");
        }
        return new TaskItem(sku.trim(), quantity, location.trim());
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskItem taskItem = (TaskItem) o;
        return quantity == taskItem.quantity
                && Objects.equals(sku, taskItem.sku)
                && Objects.equals(location, taskItem.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, quantity, location);
    }

    @Override
    public String toString() {
        return "TaskItem{"
                + "sku='"
                + sku
                + '\''
                + ", quantity="
                + quantity
                + ", location='"
                + location
                + '\''
                + '}';
    }
}
