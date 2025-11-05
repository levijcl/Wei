package com.wei.orchestrator.inventory.domain.event;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public final class InventoryAdjustedEvent {
    private final String transactionId;
    private final String warehouseId;
    private final List<TransactionLineDto> lines;
    private final LocalDateTime occurredAt;

    public InventoryAdjustedEvent(
            String transactionId,
            String warehouseId,
            List<TransactionLine> transactionLines,
            LocalDateTime occurredAt) {
        this.transactionId = transactionId;
        this.warehouseId = warehouseId;
        this.lines =
                transactionLines.stream()
                        .map(line -> new TransactionLineDto(line.getSku(), line.getQuantity()))
                        .toList();
        this.occurredAt = occurredAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public List<TransactionLineDto> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public static class TransactionLineDto {
        private final String sku;
        private final int quantity;

        public TransactionLineDto(String sku, int quantity) {
            this.sku = sku;
            this.quantity = quantity;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
