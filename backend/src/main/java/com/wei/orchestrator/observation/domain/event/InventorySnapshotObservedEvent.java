package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import java.time.LocalDateTime;
import java.util.List;

public class InventorySnapshotObservedEvent {
    private final String observerId;
    private final List<StockSnapshot> snapshots;
    private final LocalDateTime occurredAt;

    public InventorySnapshotObservedEvent(String observerId, List<StockSnapshot> snapshots) {
        this.observerId = observerId;
        this.snapshots = snapshots;
        this.occurredAt = LocalDateTime.now();
    }

    public String getObserverId() {
        return observerId;
    }

    public List<StockSnapshot> getSnapshots() {
        return snapshots;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InventorySnapshotObservedEvent{"
                + "observerId='"
                + observerId
                + '\''
                + ", snapshots="
                + snapshots
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
