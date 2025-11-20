package com.wei.orchestrator.wes.domain.event;

import com.wei.orchestrator.shared.domain.event.DomainEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class PickingTaskCreatedEvent implements DomainEvent {
    private final String taskId;
    private final String orderId;
    private final TaskOrigin origin;
    private final int priority;
    private final List<TaskItemDto> items;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    public PickingTaskCreatedEvent(
            String taskId,
            String orderId,
            TaskOrigin origin,
            int priority,
            List<TaskItemDto> items,
            LocalDateTime occurredAt) {
        this.taskId = taskId;
        this.orderId = orderId;
        this.origin = origin;
        this.priority = priority;
        this.items = items;
        this.occurredAt = occurredAt;
        this.correlationId = UUID.randomUUID();
        this.triggerContext = null;
    }

    public PickingTaskCreatedEvent(
            String taskId,
            String orderId,
            TaskOrigin origin,
            int priority,
            List<TaskItemDto> items,
            LocalDateTime occurredAt,
            TriggerContext triggerContext) {
        this.taskId = taskId;
        this.orderId = orderId;
        this.origin = origin;
        this.priority = priority;
        this.items = items;
        this.occurredAt = occurredAt;
        this.triggerContext = triggerContext;
        this.correlationId =
                triggerContext != null ? triggerContext.getCorrelationId() : UUID.randomUUID();
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOrderId() {
        return orderId;
    }

    public TaskOrigin getOrigin() {
        return origin;
    }

    public int getPriority() {
        return priority;
    }

    public List<TaskItemDto> getItems() {
        return items;
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

    public static class TaskItemDto {
        private final String sku;
        private final int quantity;
        private final String location;

        public TaskItemDto(String sku, int quantity, String location) {
            this.sku = sku;
            this.quantity = quantity;
            this.location = location;
        }

        public String getSku() {
            return sku;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getLocation() {
            return location;
        }
    }
}
