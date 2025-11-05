package com.wei.orchestrator.inventory.domain.model.valueobject;

import java.util.Objects;

public final class TransactionLine {
    private final String sku;
    private final int quantity;

    private TransactionLine(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public static TransactionLine of(String sku, int quantity) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU cannot be null or blank");
        }
        if (quantity == 0) {
            throw new IllegalArgumentException("Quantity cannot be zero");
        }
        return new TransactionLine(sku.trim(), quantity);
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isPositive() {
        return quantity > 0;
    }

    public boolean isNegative() {
        return quantity < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionLine that = (TransactionLine) o;
        return quantity == that.quantity && Objects.equals(sku, that.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, quantity);
    }

    @Override
    public String toString() {
        return String.format("TransactionLine{sku='%s', quantity=%d}", sku, quantity);
    }
}
