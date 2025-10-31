package com.wei.orchestrator.observation.domain.model;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderObserver {
    private String observerId;
    private SourceEndpoint sourceEndpoint;
    private PollingInterval pollingInterval;
    private LocalDateTime lastPolledTimestamp;
    private boolean active;
    private final List<Object> domainEvents;

    public OrderObserver() {
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public OrderObserver(
            String observerId, SourceEndpoint sourceEndpoint, PollingInterval pollingInterval) {
        if (observerId == null || observerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Observer ID cannot be null or empty");
        }
        if (sourceEndpoint == null) {
            throw new IllegalArgumentException("Source endpoint cannot be null");
        }
        if (pollingInterval == null) {
            throw new IllegalArgumentException("Polling interval cannot be null");
        }
        this.observerId = observerId;
        this.sourceEndpoint = sourceEndpoint;
        this.pollingInterval = pollingInterval;
        this.lastPolledTimestamp = null;
        this.active = true;
        this.domainEvents = new ArrayList<>();
    }

    public void pollOrderSource(OrderSourcePort orderSourcePort) {
        if (!this.shouldPoll()) {
            return;
        }

        List<ObservationResult> results =
                orderSourcePort.fetchNewOrders(this.sourceEndpoint, this.lastPolledTimestamp);

        this.lastPolledTimestamp = LocalDateTime.now();

        for (ObservationResult result : results) {
            NewOrderObservedEvent event = new NewOrderObservedEvent(this.observerId, result);
            this.domainEvents.add(event);
        }
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

    public SourceEndpoint getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(SourceEndpoint sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
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
