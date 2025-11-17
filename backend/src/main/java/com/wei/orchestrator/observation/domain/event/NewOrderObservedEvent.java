package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public class NewOrderObservedEvent implements DomainEvent {
    private final String observerId;
    private final ObservationResult observedOrder;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public NewOrderObservedEvent(String observerId, ObservationResult observedOrder) {
        this.observerId = observerId;
        this.observedOrder = observedOrder;
        this.occurredAt = LocalDateTime.now();
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public NewOrderObservedEvent(
            String observerId, ObservationResult observedOrder, TriggerContext triggerContext) {
        this.observerId = observerId;
        this.observedOrder = observedOrder;
        this.occurredAt = LocalDateTime.now();
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getObserverId() {
        return observerId;
    }

    public ObservationResult getObservedOrder() {
        return observedOrder;
    }

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public TriggerContext getTriggerContext() {
        return triggerContext;
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
                + ", correlationId="
                + correlationId
                + '}';
    }
}
