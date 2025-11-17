package com.wei.orchestrator.shared.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class TriggerContext {

    private final String triggerSource;
    private final UUID correlationId;
    private final LocalDateTime timestamp;
    private final String triggerBy;

    @JsonCreator
    private TriggerContext(
            @JsonProperty("triggerSource") String triggerSource,
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("triggerBy") String triggerBy) {
        if (triggerSource == null || triggerSource.isBlank()) {
            throw new IllegalArgumentException("Trigger source cannot be null or blank");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("Correlation ID cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.triggerSource = triggerSource;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.triggerBy = triggerBy;
    }

    public static TriggerContext fromEvent(Object event, UUID correlationId) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (correlationId == null) {
            correlationId = UUID.randomUUID();
        }
        String eventName = event.getClass().getSimpleName();
        return new TriggerContext(eventName, correlationId, LocalDateTime.now(), null);
    }

    public static TriggerContext manual() {
        return new TriggerContext("Manual", UUID.randomUUID(), LocalDateTime.now(), null);
    }

    public static TriggerContext scheduled(String schedulerName) {
        if (schedulerName == null || schedulerName.isBlank()) {
            throw new IllegalArgumentException("Scheduler name cannot be null or blank");
        }
        return new TriggerContext(
                "Scheduled:" + schedulerName, UUID.randomUUID(), LocalDateTime.now(), null);
    }

    public static TriggerContext of(String triggerSource, UUID correlationId, String triggerBy) {
        return new TriggerContext(
                triggerSource,
                correlationId != null ? correlationId : UUID.randomUUID(),
                LocalDateTime.now(),
                triggerBy);
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTriggerBy() {
        return triggerBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TriggerContext that = (TriggerContext) o;
        return Objects.equals(triggerSource, that.triggerSource)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(triggerBy, that.triggerBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(triggerSource, correlationId, timestamp, triggerBy);
    }

    @Override
    public String toString() {
        return "TriggerContext{"
                + "triggerSource='"
                + triggerSource
                + '\''
                + ", correlationId="
                + correlationId
                + ", timestamp="
                + timestamp
                + ", triggerBy='"
                + triggerBy
                + '\''
                + '}';
    }
}
