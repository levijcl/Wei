package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import java.time.LocalDateTime;

public class NewOrderObservedEvent {
    private final String observerId;
    private final ObservationResult observedOrder;
    private final LocalDateTime occurredAt;

    public NewOrderObservedEvent(String observerId, ObservationResult observedOrder) {
        this.observerId = observerId;
        this.observedOrder = observedOrder;
        this.occurredAt = LocalDateTime.now();
    }

    public String getObserverId() {
        return observerId;
    }

    public ObservationResult getObservedOrder() {
        return observedOrder;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "NewOrderObservedEvent{"
                + "observerId='"
                + observerId
                + '\''
                + ", observedOrder="
                + observedOrder
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
