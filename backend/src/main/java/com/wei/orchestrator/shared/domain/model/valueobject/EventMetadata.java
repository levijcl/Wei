package com.wei.orchestrator.shared.domain.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;

public final class EventMetadata {

    private final String context;
    private final UUID correlationId;
    private final String triggerSource;
    private final String triggerBy;

    @JsonCreator
    private EventMetadata(
            @JsonProperty("context") String context,
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("triggerSource") String triggerSource,
            @JsonProperty("triggerBy") String triggerBy) {
        if (context == null || context.isBlank()) {
            throw new IllegalArgumentException("Context cannot be null or blank");
        }
        if (correlationId == null) {
            throw new IllegalArgumentException("Correlation ID cannot be null");
        }
        if (triggerSource == null || triggerSource.isBlank()) {
            throw new IllegalArgumentException("Trigger source cannot be null or blank");
        }
        this.context = context;
        this.correlationId = correlationId;
        this.triggerSource = triggerSource;
        this.triggerBy = triggerBy;
    }

    public static EventMetadata of(
            String context, UUID correlationId, String triggerSource, String triggerBy) {
        return new EventMetadata(context, correlationId, triggerSource, triggerBy);
    }

    public static EventMetadata of(String context, UUID correlationId, String triggerSource) {
        return new EventMetadata(context, correlationId, triggerSource, null);
    }

    public String getContext() {
        return context;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public String getTriggerBy() {
        return triggerBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventMetadata that = (EventMetadata) o;
        return Objects.equals(context, that.context)
                && Objects.equals(correlationId, that.correlationId)
                && Objects.equals(triggerSource, that.triggerSource)
                && Objects.equals(triggerBy, that.triggerBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(context, correlationId, triggerSource, triggerBy);
    }

    @Override
    public String toString() {
        return "EventMetadata{"
                + "context='"
                + context
                + '\''
                + ", correlationId="
                + correlationId
                + ", triggerSource='"
                + triggerSource
                + '\''
                + ", triggerBy='"
                + triggerBy
                + '\''
                + '}';
    }
}
