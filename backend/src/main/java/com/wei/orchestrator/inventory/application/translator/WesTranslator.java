package com.wei.orchestrator.inventory.application.translator;

import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesInventoryDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

public class WesTranslator {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    public static List<StockSnapshot> toStockSnapshots(List<WesInventoryDto> wesInventories) {
        if (wesInventories == null) {
            return List.of();
        }

        return wesInventories.stream()
                .map(WesTranslator::toStockSnapshot)
                .collect(Collectors.toList());
    }

    private static StockSnapshot toStockSnapshot(WesInventoryDto wesInventory) {
        LocalDateTime timestamp = parseTimestamp(wesInventory.getUpdatedAt());

        return new StockSnapshot(
                wesInventory.getSku(),
                wesInventory.getQuantity() != null ? wesInventory.getQuantity() : 0,
                wesInventory.getWarehouseId(),
                timestamp);
    }

    private static LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.now();
        }

        try {
            return LocalDateTime.parse(timestamp, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDateTime.now();
        }
    }
}
