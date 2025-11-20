package com.wei.orchestrator.shared.application.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.model.valueobject.EventMetadata;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditRecordFactory {

    private static final Logger logger = LoggerFactory.getLogger(AuditRecordFactory.class);
    private final ObjectMapper objectMapper;

    public AuditRecordFactory() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public AuditRecord createAuditRecord(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        String context = extractContext(event);
        String aggregateType = extractAggregateType(event);
        String aggregateId = extractAggregateId(event);
        TriggerContext triggerContext = resolveTriggerContext(event);

        EventMetadata metadata =
                EventMetadata.of(
                        context,
                        event.getCorrelationId(),
                        triggerContext.getTriggerSource(),
                        triggerContext.getTriggerBy());

        String payload = serializePayload(event);

        return new AuditRecord(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventName,
                event.getOccurredAt(),
                metadata,
                payload,
                LocalDateTime.now());
    }

    private String extractContext(DomainEvent event) {
        String packageName = event.getClass().getPackageName();

        if (packageName.contains(".order.")) {
            return "Order Context";
        }
        if (packageName.contains(".wes.")) {
            return "WES Context";
        }
        if (packageName.contains(".inventory.")) {
            return "Inventory Context";
        }
        if (packageName.contains(".observation.")) {
            return "Observation Context";
        }

        return "Unknown Context";
    }

    private String extractAggregateType(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();

        String[] patterns = {
            "Order", "PickingTask", "Inventory", "Reservation",
            "ObservedOrder", "WesTask", "Discrepancy", "Adjustment",
            "Transaction", "InventoryTransaction"
        };

        for (String pattern : patterns) {
            if (eventName.contains(pattern)) {
                return pattern;
            }
        }

        return "Unknown";
    }

    private String extractAggregateId(DomainEvent event) {
        try {
            Method[] methods = event.getClass().getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.matches("get[A-Z]\\w+Id") && method.getParameterCount() == 0) {
                    Object id = method.invoke(event);
                    if (id != null) {
                        return id.toString();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(
                    "Failed to extract aggregate ID for event: {}",
                    event.getClass().getSimpleName(),
                    e);
        }

        return "UNKNOWN";
    }

    private TriggerContext resolveTriggerContext(DomainEvent event) {
        TriggerContext explicit = event.getTriggerContext();
        if (explicit != null) {
            logger.debug("Using explicit TriggerContext for {}", event.getClass().getSimpleName());
            return explicit;
        }

        logger.debug("Using manual TriggerContext for {}", event.getClass().getSimpleName());
        return TriggerContext.manual();
    }

    private String serializePayload(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            logger.error(
                    "Failed to serialize event payload: {}", event.getClass().getSimpleName(), e);
            return "{}";
        }
    }
}
