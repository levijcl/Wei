package com.wei.orchestrator.order.query.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderLineItemDto {

    private String id;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
    private ReservationInfoDto reservationInfo;
    private CommitmentInfoDto commitmentInfo;

    public OrderLineItemDto() {}

    public OrderLineItemDto(
            String id,
            String sku,
            Integer quantity,
            BigDecimal price,
            ReservationInfoDto reservationInfo,
            CommitmentInfoDto commitmentInfo) {
        this.id = id;
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
        this.reservationInfo = reservationInfo;
        this.commitmentInfo = commitmentInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public ReservationInfoDto getReservationInfo() {
        return reservationInfo;
    }

    public void setReservationInfo(ReservationInfoDto reservationInfo) {
        this.reservationInfo = reservationInfo;
    }

    public CommitmentInfoDto getCommitmentInfo() {
        return commitmentInfo;
    }

    public void setCommitmentInfo(CommitmentInfoDto commitmentInfo) {
        this.commitmentInfo = commitmentInfo;
    }

    public static class ReservationInfoDto {
        private String status;
        private String transactionId;
        private String externalReservationId;
        private String warehouseId;
        private String failureReason;
        private LocalDateTime reservedAt;

        public ReservationInfoDto() {}

        public ReservationInfoDto(
                String status,
                String transactionId,
                String externalReservationId,
                String warehouseId,
                String failureReason,
                LocalDateTime reservedAt) {
            this.status = status;
            this.transactionId = transactionId;
            this.externalReservationId = externalReservationId;
            this.warehouseId = warehouseId;
            this.failureReason = failureReason;
            this.reservedAt = reservedAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getExternalReservationId() {
            return externalReservationId;
        }

        public void setExternalReservationId(String externalReservationId) {
            this.externalReservationId = externalReservationId;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public LocalDateTime getReservedAt() {
            return reservedAt;
        }

        public void setReservedAt(LocalDateTime reservedAt) {
            this.reservedAt = reservedAt;
        }
    }

    public static class CommitmentInfoDto {
        private String status;
        private String wesTransactionId;
        private String failureReason;
        private LocalDateTime committedAt;

        public CommitmentInfoDto() {}

        public CommitmentInfoDto(
                String status,
                String wesTransactionId,
                String failureReason,
                LocalDateTime committedAt) {
            this.status = status;
            this.wesTransactionId = wesTransactionId;
            this.failureReason = failureReason;
            this.committedAt = committedAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getWesTransactionId() {
            return wesTransactionId;
        }

        public void setWesTransactionId(String wesTransactionId) {
            this.wesTransactionId = wesTransactionId;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public void setFailureReason(String failureReason) {
            this.failureReason = failureReason;
        }

        public LocalDateTime getCommittedAt() {
            return committedAt;
        }

        public void setCommittedAt(LocalDateTime committedAt) {
            this.committedAt = committedAt;
        }
    }
}
