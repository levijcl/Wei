package com.wei.orchestrator.order.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_line_item")
public class OrderLineItemEntity {
    @Id private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "sku")
    private String sku;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "reservation_status")
    private String reservationStatus;

    @Column(name = "reservation_transaction_id")
    private String reservationTransactionId;

    @Column(name = "reservation_external_reservation_id")
    private String reservationExternalReservationId;

    @Column(name = "reservation_warehouse_id")
    private String reservationWarehouseId;

    @Column(name = "reservation_failure_reason")
    private String reservationFailureReason;

    @Column(name = "reservation_reserved_at")
    private java.time.LocalDateTime reservationReservedAt;

    @Column(name = "commitment_status")
    private String commitmentStatus;

    @Column(name = "commitment_wes_transaction_id")
    private String commitmentWesTransactionId;

    @Column(name = "commitment_failure_reason")
    private String commitmentFailureReason;

    @Column(name = "commitment_committed_at")
    private java.time.LocalDateTime commitmentCommittedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrderLineItemEntity() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrderEntity getOrder() {
        return order;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
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

    public String getReservationStatus() {
        return reservationStatus;
    }

    public void setReservationStatus(String reservationStatus) {
        this.reservationStatus = reservationStatus;
    }

    public String getReservationTransactionId() {
        return reservationTransactionId;
    }

    public void setReservationTransactionId(String reservationTransactionId) {
        this.reservationTransactionId = reservationTransactionId;
    }

    public String getReservationExternalReservationId() {
        return reservationExternalReservationId;
    }

    public void setReservationExternalReservationId(String reservationExternalReservationId) {
        this.reservationExternalReservationId = reservationExternalReservationId;
    }

    public String getReservationWarehouseId() {
        return reservationWarehouseId;
    }

    public void setReservationWarehouseId(String reservationWarehouseId) {
        this.reservationWarehouseId = reservationWarehouseId;
    }

    public String getReservationFailureReason() {
        return reservationFailureReason;
    }

    public void setReservationFailureReason(String reservationFailureReason) {
        this.reservationFailureReason = reservationFailureReason;
    }

    public java.time.LocalDateTime getReservationReservedAt() {
        return reservationReservedAt;
    }

    public void setReservationReservedAt(java.time.LocalDateTime reservationReservedAt) {
        this.reservationReservedAt = reservationReservedAt;
    }

    public String getCommitmentStatus() {
        return commitmentStatus;
    }

    public void setCommitmentStatus(String commitmentStatus) {
        this.commitmentStatus = commitmentStatus;
    }

    public String getCommitmentWesTransactionId() {
        return commitmentWesTransactionId;
    }

    public void setCommitmentWesTransactionId(String commitmentWesTransactionId) {
        this.commitmentWesTransactionId = commitmentWesTransactionId;
    }

    public String getCommitmentFailureReason() {
        return commitmentFailureReason;
    }

    public void setCommitmentFailureReason(String commitmentFailureReason) {
        this.commitmentFailureReason = commitmentFailureReason;
    }

    public java.time.LocalDateTime getCommitmentCommittedAt() {
        return commitmentCommittedAt;
    }

    public void setCommitmentCommittedAt(java.time.LocalDateTime commitmentCommittedAt) {
        this.commitmentCommittedAt = commitmentCommittedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
