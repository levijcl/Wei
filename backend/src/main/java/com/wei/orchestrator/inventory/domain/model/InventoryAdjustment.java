package com.wei.orchestrator.inventory.domain.model;

import com.wei.orchestrator.inventory.domain.event.InventoryAdjustmentAppliedEvent;
import com.wei.orchestrator.inventory.domain.event.InventoryDiscrepancyDetectedEvent;
import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InventoryAdjustment {
    private String adjustmentId;
    private AdjustmentStatus status;
    private List<DiscrepancyLog> discrepancyLogs;
    private String appliedTransactionId;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private final List<Object> domainEvents = new ArrayList<>();

    public InventoryAdjustment() {}

    public static InventoryAdjustment detectDiscrepancy(
            List<StockSnapshot> inventorySnapshots, List<StockSnapshot> wesSnapshots) {

        if (inventorySnapshots == null || wesSnapshots == null) {
            throw new IllegalArgumentException("Snapshots cannot be null");
        }

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.adjustmentId = UUID.randomUUID().toString();
        adjustment.status = AdjustmentStatus.PENDING;
        adjustment.createdAt = LocalDateTime.now();
        adjustment.discrepancyLogs = new ArrayList<>();

        Map<String, StockSnapshot> inventoryMap = new HashMap<>();
        for (StockSnapshot snapshot : inventorySnapshots) {
            String key = snapshot.getWarehouseId() + ":" + snapshot.getSku();
            inventoryMap.put(key, snapshot);
        }

        Map<String, StockSnapshot> wesMap = new HashMap<>();
        for (StockSnapshot snapshot : wesSnapshots) {
            String key = snapshot.getWarehouseId() + ":" + snapshot.getSku();
            wesMap.put(key, snapshot);
        }

        for (StockSnapshot wesSnapshot : wesSnapshots) {
            String key = wesSnapshot.getWarehouseId() + ":" + wesSnapshot.getSku();
            StockSnapshot inventorySnapshot = inventoryMap.get(key);

            int expectedQuantity = wesSnapshot.getQuantity();
            int actualQuantity = inventorySnapshot != null ? inventorySnapshot.getQuantity() : 0;

            if (expectedQuantity != actualQuantity) {
                DiscrepancyLog log =
                        DiscrepancyLog.of(
                                wesSnapshot.getSku(),
                                wesSnapshot.getWarehouseId(),
                                expectedQuantity,
                                actualQuantity);
                adjustment.discrepancyLogs.add(log);
            }
        }

        for (StockSnapshot inventorySnapshot : inventorySnapshots) {
            String key = inventorySnapshot.getWarehouseId() + ":" + inventorySnapshot.getSku();
            if (!wesMap.containsKey(key) && inventorySnapshot.getQuantity() > 0) {
                DiscrepancyLog log =
                        DiscrepancyLog.of(
                                inventorySnapshot.getSku(),
                                inventorySnapshot.getWarehouseId(),
                                0,
                                inventorySnapshot.getQuantity());
                adjustment.discrepancyLogs.add(log);
            }
        }

        if (!adjustment.discrepancyLogs.isEmpty()) {
            adjustment.addDomainEvent(
                    new InventoryDiscrepancyDetectedEvent(
                            adjustment.adjustmentId,
                            adjustment.discrepancyLogs,
                            adjustment.createdAt));
        }

        return adjustment;
    }

    public void markAsProcessing() {
        if (!status.canProcess()) {
            throw new IllegalStateException("Cannot process adjustment in status: " + status);
        }
        this.status = AdjustmentStatus.PROCESSING;
    }

    public void applyAdjustment(String transactionId) {
        if (!status.canProcess() && !status.canComplete()) {
            throw new IllegalStateException("Cannot apply adjustment in status: " + status);
        }
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank");
        }

        this.status = AdjustmentStatus.PROCESSING;
        this.appliedTransactionId = transactionId;

        addDomainEvent(
                new InventoryAdjustmentAppliedEvent(
                        this.adjustmentId, transactionId, LocalDateTime.now()));
    }

    public void complete() {
        if (!status.canComplete()) {
            throw new IllegalStateException("Cannot complete adjustment in status: " + status);
        }

        this.status = AdjustmentStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        if (!status.canFail()) {
            throw new IllegalStateException("Cannot fail adjustment in status: " + status);
        }

        this.status = AdjustmentStatus.FAILED;
        this.failureReason = reason;
        this.processedAt = LocalDateTime.now();
    }

    public boolean hasDiscrepancies() {
        return discrepancyLogs != null && !discrepancyLogs.isEmpty();
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

    public String getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(String adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public AdjustmentStatus getStatus() {
        return status;
    }

    public void setStatus(AdjustmentStatus status) {
        this.status = status;
    }

    public List<DiscrepancyLog> getDiscrepancyLogs() {
        return discrepancyLogs != null ? new ArrayList<>(discrepancyLogs) : new ArrayList<>();
    }

    public void setDiscrepancyLogs(List<DiscrepancyLog> discrepancyLogs) {
        this.discrepancyLogs = discrepancyLogs;
    }

    public String getAppliedTransactionId() {
        return appliedTransactionId;
    }

    public void setAppliedTransactionId(String appliedTransactionId) {
        this.appliedTransactionId = appliedTransactionId;
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

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
