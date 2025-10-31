package com.wei.orchestrator.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public class CreateOrderRequest {
    @NotBlank(message = "Order ID is required") private String orderId;

    @NotEmpty(message = "Order must have at least one item") @Valid private List<OrderLineItemDto> items;

    public CreateOrderRequest() {}

    public CreateOrderRequest(String orderId, List<OrderLineItemDto> items) {
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
        @NotBlank(message = "SKU is required") private String sku;

        @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") private Integer quantity;

        @NotNull(message = "Price is required") @PositiveOrZero(message = "Price cannot be negative") private BigDecimal price;

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
