package com.wei.orchestrator.inventory.infrastructure.persistence;

import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory_adjustments")
public class InventoryAdjustmentEntity {

    @Id
    @Column(name = "adjustment_id", length = 100)
    private String adjustmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AdjustmentStatus status;

    @Column(name = "applied_transaction_id", length = 100)
    private String appliedTransactionId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscrepancyLogEntity> discrepancyLogs = new ArrayList<>();

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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DiscrepancyLogEntity> getDiscrepancyLogs() {
        return discrepancyLogs;
    }

    public void setDiscrepancyLogs(List<DiscrepancyLogEntity> discrepancyLogs) {
        this.discrepancyLogs = discrepancyLogs;
    }

    public void addDiscrepancyLog(DiscrepancyLogEntity log) {
        discrepancyLogs.add(log);
        log.setAdjustment(this);
    }

    public void removeDiscrepancyLog(DiscrepancyLogEntity log) {
        discrepancyLogs.remove(log);
        log.setAdjustment(null);
    }
}
