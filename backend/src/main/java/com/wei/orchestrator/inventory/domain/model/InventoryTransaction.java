package com.wei.orchestrator.inventory.domain.model;

import com.wei.orchestrator.inventory.domain.event.*;
import com.wei.orchestrator.inventory.domain.model.valueobject.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class InventoryTransaction {
    private String transactionId;
    private TransactionType type;
    private TransactionStatus status;
    private TransactionSource source;
    private String sourceReferenceId;
    private WarehouseLocation warehouseLocation;
    private List<TransactionLine> transactionLines;
    private ExternalReservationId externalReservationId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private final List<Object> domainEvents = new ArrayList<>();

    public InventoryTransaction() {}

    public static InventoryTransaction createReservation(
            String orderId, String sku, String warehouseId, int quantity) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for reservation");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.transactionId = UUID.randomUUID().toString();
        transaction.type = TransactionType.OUTBOUND;
        transaction.status = TransactionStatus.PENDING;
        transaction.source = TransactionSource.ORDER_RESERVATION;
        transaction.sourceReferenceId = orderId;
        transaction.warehouseLocation = WarehouseLocation.of(warehouseId);
        transaction.createdAt = LocalDateTime.now();

        List<TransactionLine> lines = new ArrayList<>();
        lines.add(TransactionLine.of(sku, quantity));
        transaction.transactionLines = lines;

        transaction.addDomainEvent(
                new InventoryReservationRequestedEvent(
                        transaction.transactionId,
                        orderId,
                        sku,
                        warehouseId,
                        quantity,
                        transaction.createdAt));

        return transaction;
    }

    public static InventoryTransaction createOutboundTransaction(
            String sourceReferenceId,
            TransactionSource source,
            String warehouseId,
            List<TransactionLine> lines,
            ExternalReservationId externalReservationId) {

        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Transaction must have at least one line");
        }
        for (TransactionLine line : lines) {
            if (line.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for OUTBOUND transaction");
            }
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.transactionId = UUID.randomUUID().toString();
        transaction.type = TransactionType.OUTBOUND;
        transaction.status = TransactionStatus.PENDING;
        transaction.source = source;
        transaction.sourceReferenceId = sourceReferenceId;
        transaction.warehouseLocation = WarehouseLocation.of(warehouseId);
        transaction.transactionLines = new ArrayList<>(lines);
        transaction.externalReservationId = externalReservationId;
        transaction.createdAt = LocalDateTime.now();

        transaction.addDomainEvent(
                new InventoryTransactionCreatedEvent(
                        transaction.transactionId,
                        TransactionType.OUTBOUND,
                        sourceReferenceId,
                        source,
                        transaction.createdAt));

        return transaction;
    }

    public static InventoryTransaction createInboundTransaction(
            String sourceReferenceId,
            TransactionSource source,
            String warehouseId,
            List<TransactionLine> lines) {

        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Transaction must have at least one line");
        }
        for (TransactionLine line : lines) {
            if (line.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for INBOUND transaction");
            }
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.transactionId = UUID.randomUUID().toString();
        transaction.type = TransactionType.INBOUND;
        transaction.status = TransactionStatus.PENDING;
        transaction.source = source;
        transaction.sourceReferenceId = sourceReferenceId;
        transaction.warehouseLocation = WarehouseLocation.of(warehouseId);
        transaction.transactionLines = new ArrayList<>(lines);
        transaction.createdAt = LocalDateTime.now();

        transaction.addDomainEvent(
                new InventoryTransactionCreatedEvent(
                        transaction.transactionId,
                        TransactionType.INBOUND,
                        sourceReferenceId,
                        source,
                        transaction.createdAt));

        return transaction;
    }

    public static InventoryTransaction createAdjustmentTransaction(
            String sourceReferenceId,
            TransactionSource source,
            String warehouseId,
            List<TransactionLine> lines) {

        if (sourceReferenceId == null || sourceReferenceId.isBlank()) {
            throw new IllegalArgumentException("Source reference ID cannot be null or blank");
        }
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Transaction must have at least one line");
        }

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.transactionId = UUID.randomUUID().toString();
        transaction.type = TransactionType.ADJUSTMENT;
        transaction.status = TransactionStatus.PENDING;
        transaction.source = source;
        transaction.sourceReferenceId = sourceReferenceId;
        transaction.warehouseLocation = WarehouseLocation.of(warehouseId);
        transaction.transactionLines = new ArrayList<>(lines);
        transaction.createdAt = LocalDateTime.now();

        transaction.addDomainEvent(
                new InventoryTransactionCreatedEvent(
                        transaction.transactionId,
                        TransactionType.ADJUSTMENT,
                        sourceReferenceId,
                        source,
                        transaction.createdAt));

        return transaction;
    }

    public void markAsReserved(ExternalReservationId externalReservationId) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("Can only mark PENDING transaction as reserved");
        }
        if (this.source != TransactionSource.ORDER_RESERVATION) {
            throw new IllegalStateException(
                    "Only ORDER_RESERVATION transactions can be marked as reserved");
        }

        this.externalReservationId = externalReservationId;
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        addDomainEvent(
                new InventoryReservedEvent(
                        this.transactionId,
                        this.sourceReferenceId,
                        externalReservationId.getValue(),
                        this.completedAt));
    }

    public void markAsProcessing() {
        if (!status.canProcess()) {
            throw new IllegalStateException("Cannot process transaction in status: " + status);
        }
        this.status = TransactionStatus.PROCESSING;
    }

    public void complete() {
        if (!status.canComplete()) {
            throw new IllegalStateException("Cannot complete transaction in status: " + status);
        }

        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        if (type == TransactionType.INBOUND) {
            addDomainEvent(
                    new InventoryIncreasedEvent(
                            this.transactionId,
                            this.warehouseLocation.getWarehouseId(),
                            this.transactionLines,
                            this.completedAt));
        } else if (type == TransactionType.OUTBOUND) {
            if (source == TransactionSource.RESERVATION_CONSUMED && externalReservationId != null) {
                addDomainEvent(
                        new ReservationConsumedEvent(
                                this.transactionId,
                                this.sourceReferenceId,
                                this.externalReservationId.getValue(),
                                this.completedAt));
            }
            addDomainEvent(
                    new InventoryDecreasedEvent(
                            this.transactionId,
                            this.warehouseLocation.getWarehouseId(),
                            this.transactionLines,
                            this.completedAt));
        } else if (type == TransactionType.ADJUSTMENT) {
            addDomainEvent(
                    new InventoryAdjustedEvent(
                            this.transactionId,
                            this.warehouseLocation.getWarehouseId(),
                            this.transactionLines,
                            this.completedAt));
        }

        addDomainEvent(
                new InventoryTransactionCompletedEvent(
                        this.transactionId,
                        this.type,
                        this.source,
                        this.sourceReferenceId,
                        this.completedAt));
    }

    public void fail(String reason) {
        if (!status.canFail()) {
            throw new IllegalStateException("Cannot fail transaction in status: " + status);
        }

        this.status = TransactionStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();

        if (source == TransactionSource.ORDER_RESERVATION) {
            addDomainEvent(
                    new ReservationFailedEvent(
                            this.transactionId, this.sourceReferenceId, reason, this.completedAt));
        }

        addDomainEvent(
                new InventoryTransactionFailedEvent(
                        this.transactionId, this.type, this.source, reason, this.completedAt));
    }

    public void releaseReservation() {
        if (this.externalReservationId == null) {
            throw new IllegalStateException(
                    "Cannot release reservation - no external reservation ID");
        }
        if (this.status.isTerminal()) {
            throw new IllegalStateException(
                    "Cannot release reservation in terminal status: " + status);
        }

        this.status = TransactionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        addDomainEvent(
                new ReservationReleasedEvent(
                        this.transactionId,
                        this.sourceReferenceId,
                        this.externalReservationId.getValue(),
                        this.completedAt));

        addDomainEvent(
                new InventoryTransactionCompletedEvent(
                        this.transactionId,
                        this.type,
                        TransactionSource.RESERVATION_RELEASED,
                        this.sourceReferenceId,
                        this.completedAt));
    }

    private void addDomainEvent(Object event) {
        this.domainEvents.add(event);
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public TransactionSource getSource() {
        return source;
    }

    public void setSource(TransactionSource source) {
        this.source = source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(String sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }

    public WarehouseLocation getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(WarehouseLocation warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public List<TransactionLine> getTransactionLines() {
        return transactionLines != null ? new ArrayList<>(transactionLines) : new ArrayList<>();
    }

    public void setTransactionLines(List<TransactionLine> transactionLines) {
        this.transactionLines = transactionLines;
    }

    public ExternalReservationId getExternalReservationId() {
        return externalReservationId;
    }

    public void setExternalReservationId(ExternalReservationId externalReservationId) {
        this.externalReservationId = externalReservationId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
