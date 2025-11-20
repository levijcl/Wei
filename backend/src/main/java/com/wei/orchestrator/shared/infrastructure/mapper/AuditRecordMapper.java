package com.wei.orchestrator.shared.infrastructure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.model.valueobject.EventMetadata;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.util.UUID;

public class AuditRecordMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static AuditRecordEntity toEntity(AuditRecord domain) {
        if (domain == null) {
            return null;
        }

        AuditRecordEntity entity = new AuditRecordEntity();
        entity.setRecordId(domain.getRecordId().toString());
        entity.setAggregateType(domain.getAggregateType());
        entity.setAggregateId(domain.getAggregateId());
        entity.setEventName(domain.getEventName());
        entity.setEventTimestamp(domain.getEventTimestamp());

        try {
            entity.setEventMetadata(objectMapper.writeValueAsString(domain.getEventMetadata()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize EventMetadata to JSON", e);
        }

        entity.setPayload(domain.getPayload());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    public static AuditRecord toDomain(AuditRecordEntity entity) {
        if (entity == null) {
            return null;
        }

        EventMetadata eventMetadata;
        try {
            eventMetadata = objectMapper.readValue(entity.getEventMetadata(), EventMetadata.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize EventMetadata from JSON", e);
        }

        return new AuditRecord(
                UUID.fromString(entity.getRecordId()),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventName(),
                entity.getEventTimestamp(),
                eventMetadata,
                entity.getPayload(),
                entity.getCreatedAt());
    }
}
