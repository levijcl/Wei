package com.wei.orchestrator.shared.domain.event;

import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

    default UUID getCorrelationId() {
        return UUID.randomUUID();
    }

    default LocalDateTime getOccurredAt() {
        return LocalDateTime.now();
    }

    default TriggerContext getTriggerContext() {
        return null;
    }
}
