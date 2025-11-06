package com.wei.orchestrator.order.application.command;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class CreateOrderCommand {
    private String orderId;
    private List<OrderLineItemDto> items;
    private LocalDateTime scheduledPickupTime;
    private Duration fulfillmentLeadTime;

    public CreateOrderCommand() {}

    public CreateOrderCommand(String orderId, List<OrderLineItemDto> items) {
        this(orderId, items, null, null);
    }

    public CreateOrderCommand(
            String orderId,
            List<OrderLineItemDto> items,
            LocalDateTime scheduledPickupTime,
            Duration fulfillmentLeadTime) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        for (OrderLineItemDto item : items) {
            if (item.getSku() == null || item.getSku().isBlank()) {
                throw new IllegalArgumentException("SKU cannot be null or empty");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price cannot be negative");
            }
        }

        this.orderId = orderId;
        this.items = items;
        this.scheduledPickupTime = scheduledPickupTime;
        this.fulfillmentLeadTime = fulfillmentLeadTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderLineItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderLineItemDto> items) {
        this.items = items;
    }

    public LocalDateTime getScheduledPickupTime() {
        return scheduledPickupTime;
    }

    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) {
        this.scheduledPickupTime = scheduledPickupTime;
    }

    public Duration getFulfillmentLeadTime() {
        return fulfillmentLeadTime;
    }

    public void setFulfillmentLeadTime(Duration fulfillmentLeadTime) {
        this.fulfillmentLeadTime = fulfillmentLeadTime;
    }

    public static class OrderLineItemDto {
        private String sku;
        private Integer quantity;
        private BigDecimal price;

        public OrderLineItemDto() {}

        public OrderLineItemDto(String sku, Integer quantity, BigDecimal price) {
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
}
