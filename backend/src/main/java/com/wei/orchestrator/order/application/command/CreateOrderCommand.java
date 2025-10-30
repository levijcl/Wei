package com.wei.orchestrator.order.application.command;

import java.math.BigDecimal;
import java.util.List;

public class CreateOrderCommand {
    private String orderId;
    private List<OrderLineItemDto> items;

    public CreateOrderCommand() {}

    public CreateOrderCommand(String orderId, List<OrderLineItemDto> items) {
        this.orderId = orderId;
        this.items = items;
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
