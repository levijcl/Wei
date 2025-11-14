package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public final class InventoryDiscrepancyDetectedEvent {
    private final String adjustmentId;
    private final List<DiscrepancyLogDto> discrepancies;
    private final LocalDateTime occurredAt;

    public InventoryDiscrepancyDetectedEvent(
            String adjustmentId, List<DiscrepancyLog> discrepancyLogs, LocalDateTime occurredAt) {
        this.adjustmentId = adjustmentId;
        this.discrepancies =
                discrepancyLogs.stream()
                        .map(
                                log ->
                                        new DiscrepancyLogDto(
                                                log.getSku(),
                                                log.getWarehouseId(),
                                                log.getExpectedQuantity(),
                                                log.getActualQuantity(),
                                                log.getDifference()))
                        .toList();
        this.occurredAt = occurredAt;
    }

    public String getAdjustmentId() {
        return adjustmentId;
    }

    public List<DiscrepancyLogDto> getDiscrepancies() {
        return Collections.unmodifiableList(discrepancies);
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public static class DiscrepancyLogDto {
        private final String sku;
        private final String warehouseId;
        private final int expectedQuantity;
        private final int actualQuantity;
        private final int difference;

        public DiscrepancyLogDto(
                String sku,
                String warehouseId,
                int expectedQuantity,
                int actualQuantity,
                int difference) {
            this.sku = sku;
            this.warehouseId = warehouseId;
            this.expectedQuantity = expectedQuantity;
            this.actualQuantity = actualQuantity;
            this.difference = difference;
        }

        public String getSku() {
            return sku;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public int getExpectedQuantity() {
            return expectedQuantity;
        }

        public int getActualQuantity() {
            return actualQuantity;
        }

        public int getDifference() {
            return difference;
        }
    }
}
