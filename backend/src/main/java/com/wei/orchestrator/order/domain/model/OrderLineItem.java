package com.wei.orchestrator.order.domain.model;

import java.math.BigDecimal;

public class OrderLineItem {
    private String sku;
    private Integer quantity;
    private BigDecimal price;

    public OrderLineItem() {}

    public OrderLineItem(String sku, Integer quantity, BigDecimal price) {
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
