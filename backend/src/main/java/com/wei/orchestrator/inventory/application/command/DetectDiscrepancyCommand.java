package com.wei.orchestrator.inventory.application.command;

import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DetectDiscrepancyCommand {
    private final String observerId;
    private final List<StockSnapshot> inventorySnapshots;

    public DetectDiscrepancyCommand(String observerId, List<StockSnapshot> inventorySnapshots) {
        if (observerId == null || observerId.isBlank()) {
            throw new IllegalArgumentException("Observer ID cannot be null or blank");
        }
        if (inventorySnapshots == null || inventorySnapshots.isEmpty()) {
            throw new IllegalArgumentException("Inventory snapshots cannot be null or empty");
        }

        this.observerId = observerId;
        this.inventorySnapshots = new ArrayList<>(inventorySnapshots);
    }

    public String getObserverId() {
        return observerId;
    }

    public List<StockSnapshot> getInventorySnapshots() {
        return new ArrayList<>(inventorySnapshots);
    }
}
