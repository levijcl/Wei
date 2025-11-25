package com.wei.orchestrator.inventory.query.dto;

import java.time.LocalDateTime;
import java.util.List;

public class InventoryTransactionDetailDto {

    private String transactionId;
    private String type;
    private String status;
    private String source;
    private String sourceReferenceId;
    private WarehouseLocationDto warehouseLocation;
    private List<TransactionLineDto> transactionLines;
    private String externalReservationId;
    private String relatedTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public InventoryTransactionDetailDto() {}

    public InventoryTransactionDetailDto(
            String transactionId,
            String type,
            String status,
            String source,
            String sourceReferenceId,
            WarehouseLocationDto warehouseLocation,
            List<TransactionLineDto> transactionLines,
            String externalReservationId,
            String relatedTransactionId,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            LocalDateTime updatedAt) {
        this.transactionId = transactionId;
        this.type = type;
        this.status = status;
        this.source = source;
        this.sourceReferenceId = sourceReferenceId;
        this.warehouseLocation = warehouseLocation;
        this.transactionLines = transactionLines;
        this.externalReservationId = externalReservationId;
        this.relatedTransactionId = relatedTransactionId;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceReferenceId() {
        return sourceReferenceId;
    }

    public void setSourceReferenceId(String sourceReferenceId) {
        this.sourceReferenceId = sourceReferenceId;
    }

    public WarehouseLocationDto getWarehouseLocation() {
        return warehouseLocation;
    }

    public void setWarehouseLocation(WarehouseLocationDto warehouseLocation) {
        this.warehouseLocation = warehouseLocation;
    }

    public List<TransactionLineDto> getTransactionLines() {
        return transactionLines;
    }

    public void setTransactionLines(List<TransactionLineDto> transactionLines) {
        this.transactionLines = transactionLines;
    }

    public String getExternalReservationId() {
        return externalReservationId;
    }

    public void setExternalReservationId(String externalReservationId) {
        this.externalReservationId = externalReservationId;
    }

    public String getRelatedTransactionId() {
        return relatedTransactionId;
    }

    public void setRelatedTransactionId(String relatedTransactionId) {
        this.relatedTransactionId = relatedTransactionId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
