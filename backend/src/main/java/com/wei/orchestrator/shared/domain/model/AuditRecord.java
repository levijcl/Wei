package com.wei.orchestrator.shared.domain.model;

import com.wei.orchestrator.shared.domain.model.valueobject.EventMetadata;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class AuditRecord {

    private final UUID recordId;
    private final String aggregateType;
    private final String aggregateId;
    private final String eventName;
    private final LocalDateTime eventTimestamp;
    private final EventMetadata eventMetadata;
    private final String payload;
    private final LocalDateTime createdAt;

    public AuditRecord(
            UUID recordId,
            String aggregateType,
            String aggregateId,
            String eventName,
            LocalDateTime eventTimestamp,
            EventMetadata eventMetadata,
            String payload,
            LocalDateTime createdAt) {

        if (recordId == null) {
            throw new IllegalArgumentException("Record ID cannot be null");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or blank");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or blank");
        }
        if (eventName == null || eventName.isBlank()) {
            throw new IllegalArgumentException("Event name cannot be null or blank");
        }
        if (eventTimestamp == null) {
            throw new IllegalArgumentException("Event timestamp cannot be null");
        }
        if (eventMetadata == null) {
            throw new IllegalArgumentException("Event metadata cannot be null");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created at cannot be null");
        }

        this.recordId = recordId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventName = eventName;
        this.eventTimestamp = eventTimestamp;
        this.eventMetadata = eventMetadata;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public UUID getRecordId() {
        return recordId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventName() {
        return eventName;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public EventMetadata getEventMetadata() {
        return eventMetadata;
    }

    public String getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditRecord that = (AuditRecord) o;
        return Objects.equals(recordId, that.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }

    @Override
    public String toString() {
        return "AuditRecord{"
                + "recordId="
                + recordId
                + ", aggregateType='"
                + aggregateType
                + '\''
                + ", aggregateId='"
                + aggregateId
                + '\''
                + ", eventName='"
                + eventName
                + '\''
                + ", eventTimestamp="
                + eventTimestamp
                + ", createdAt="
                + createdAt
                + '}';
    }
}
