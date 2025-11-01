package com.wei.orchestrator.observation.domain.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public class ObservedOrderItem {
    private final String sku;
    private final String productName;
    private final int quantity;
    private final BigDecimal price;

    public ObservedOrderItem(String sku, String productName, int quantity, BigDecimal price) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("SKU cannot be null or empty");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be null or negative");
        }
        this.sku = sku;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservedOrderItem that = (ObservedOrderItem) o;
        return quantity == that.quantity
                && Objects.equals(sku, that.sku)
                && Objects.equals(productName, that.productName)
                && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku, productName, quantity, price);
    }

    @Override
    public String toString() {
        return "ObservedOrderItem{"
                + "sku='"
                + sku
                + '\''
                + ", productName='"
                + productName
                + '\''
                + ", quantity="
                + quantity
                + ", price="
                + price
                + '}';
    }
}
