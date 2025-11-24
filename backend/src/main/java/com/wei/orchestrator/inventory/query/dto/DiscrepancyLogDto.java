package com.wei.orchestrator.inventory.query.dto;

import java.time.LocalDateTime;

public class DiscrepancyLogDto {
    private Long id;
    private String sku;
    private String warehouseId;
    private Integer expectedQuantity;
    private Integer actualQuantity;
    private Integer difference;
    private String discrepancyType;
    private LocalDateTime detectedAt;

    public DiscrepancyLogDto() {}

    public DiscrepancyLogDto(
            Long id,
            String sku,
            String warehouseId,
            Integer expectedQuantity,
            Integer actualQuantity,
            Integer difference,
            LocalDateTime detectedAt) {
        this.id = id;
        this.sku = sku;
        this.warehouseId = warehouseId;
        this.expectedQuantity = expectedQuantity;
        this.actualQuantity = actualQuantity;
        this.difference = difference;
        this.discrepancyType =
                difference > 0 ? "OVERSTOCK" : (difference < 0 ? "UNDERSTOCK" : "NONE");
        this.detectedAt = detectedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Integer expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Integer getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(Integer actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public Integer getDifference() {
        return difference;
    }

    public void setDifference(Integer difference) {
        this.difference = difference;
    }

    public String getDiscrepancyType() {
        return discrepancyType;
    }

    public void setDiscrepancyType(String discrepancyType) {
        this.discrepancyType = discrepancyType;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }
}
