package com.wei.orchestrator.inventory.query.dto;

import java.time.LocalDateTime;

public class InventoryAdjustmentSummaryDto {
    private String adjustmentId;
    private String status;
    private Integer discrepancyCount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public InventoryAdjustmentSummaryDto() {}

    public InventoryAdjustmentSummaryDto(
            String adjustmentId,
            String status,
            Integer discrepancyCount,
            Integer totalDifference,
            LocalDateTime createdAt,
            LocalDateTime processedAt) {
        this.adjustmentId = adjustmentId;
        this.status = status;
        this.discrepancyCount = discrepancyCount;
        this.description =
                discrepancyCount + " SKUs / " + Math.abs(totalDifference) + " pcs total difference";
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public String getAdjustmentId() {
        return adjustmentId;
    }

    public void setAdjustmentId(String adjustmentId) {
        this.adjustmentId = adjustmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDiscrepancyCount() {
        return discrepancyCount;
    }

    public void setDiscrepancyCount(Integer discrepancyCount) {
        this.discrepancyCount = discrepancyCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
