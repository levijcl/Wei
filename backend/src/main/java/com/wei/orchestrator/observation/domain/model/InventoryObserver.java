package com.wei.orchestrator.observation.domain.model;

import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.infrastructure.adapter.dto.InventorySnapshotDto;
import com.wei.orchestrator.observation.domain.event.InventorySnapshotObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationRule;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.StockSnapshot;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryObserver {
    private String observerId;
    private ObservationRule observationRule;
    private PollingInterval pollingInterval;
    private LocalDateTime lastPolledTimestamp;
    private boolean active;
    private final List<Object> domainEvents;

    public InventoryObserver() {
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public InventoryObserver(
            String observerId, ObservationRule observationRule, PollingInterval pollingInterval) {
        if (observerId == null || observerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Observer ID cannot be null or empty");
        }
        if (observationRule == null) {
            throw new IllegalArgumentException("Observation rule cannot be null");
        }
        if (pollingInterval == null) {
            throw new IllegalArgumentException("Polling interval cannot be null");
        }
        this.observerId = observerId;
        this.observationRule = observationRule;
        this.pollingInterval = pollingInterval;
        this.lastPolledTimestamp = null;
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public void pollInventorySnapshot(InventoryPort inventoryPort) {
        if (!this.shouldPoll()) {
            return;
        }

        List<InventorySnapshotDto> dtos = inventoryPort.getInventorySnapshot();

        List<StockSnapshot> snapshots =
                dtos.stream().map(this::mapToStockSnapshot).collect(Collectors.toList());

        this.lastPolledTimestamp = LocalDateTime.now();

        InventorySnapshotObservedEvent event =
                new InventorySnapshotObservedEvent(this.observerId, snapshots);
        this.domainEvents.add(event);
    }

    private StockSnapshot mapToStockSnapshot(InventorySnapshotDto dto) {
        return new StockSnapshot(
                dto.getSku(),
                dto.getAvailableQuantity(),
                dto.getWarehouseId(),
                LocalDateTime.parse(dto.getUpdatedAt()));
    }

    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean shouldPoll() {
        if (!active) {
            return false;
        }
        if (lastPolledTimestamp == null) {
            return true;
        }
        LocalDateTime nextPollTime = lastPolledTimestamp.plusSeconds(pollingInterval.getSeconds());
        return LocalDateTime.now().isAfter(nextPollTime);
    }

    public String getObserverId() {
        return observerId;
    }

    public void setObserverId(String observerId) {
        this.observerId = observerId;
    }

    public ObservationRule getObservationRule() {
        return observationRule;
    }

    public void setObservationRule(ObservationRule observationRule) {
        this.observationRule = observationRule;
    }

    public PollingInterval getPollingInterval() {
        return pollingInterval;
    }

    public void setPollingInterval(PollingInterval pollingInterval) {
        this.pollingInterval = pollingInterval;
    }

    public LocalDateTime getLastPolledTimestamp() {
        return lastPolledTimestamp;
    }

    public void setLastPolledTimestamp(LocalDateTime lastPolledTimestamp) {
        this.lastPolledTimestamp = lastPolledTimestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
