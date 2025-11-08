package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateReservationResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private ReservationData data;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ReservationData getData() {
        return data;
    }

    public void setData(ReservationData data) {
        this.data = data;
    }

    public static class ReservationData {

        @JsonProperty("RESERVATION_ID")
        private String reservationId;

        @JsonProperty("SKU")
        private String sku;

        @JsonProperty("WAREHOUSE_ID")
        private String warehouseId;

        @JsonProperty("ORDER_ID")
        private String orderId;

        @JsonProperty("QUANTITY")
        private Integer quantity;

        @JsonProperty("STATUS")
        private String status;

        @JsonProperty("CREATED_AT")
        private String createdAt;

        @JsonProperty("CONSUMED_AT")
        private String consumedAt;

        @JsonProperty("RELEASED_AT")
        private String releasedAt;

        @JsonProperty("PRODUCT_NAME")
        private String productName;

        public String getReservationId() {
            return reservationId;
        }

        public void setReservationId(String reservationId) {
            this.reservationId = reservationId;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getConsumedAt() {
            return consumedAt;
        }

        public void setConsumedAt(String consumedAt) {
            this.consumedAt = consumedAt;
        }

        public String getReleasedAt() {
            return releasedAt;
        }

        public void setReleasedAt(String releasedAt) {
            this.releasedAt = releasedAt;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }
    }
}
