package com.wei.orchestrator.unit.order.query.helper;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.query.dto.OrderProcessStatusDto;
import com.wei.orchestrator.order.query.helper.ProcessStep;
import com.wei.orchestrator.shared.infrastructure.persistence.AuditRecordEntity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProcessStepTest {

    @Nested
    class FilterEventsTest {

        @Test
        void shouldFilterSingleEventForOrderReceived() {
            List<AuditRecordEntity> auditRecords = new ArrayList<>();
            auditRecords.add(createAuditRecord("NewOrderObservedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("OrderScheduledEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("InventoryReservedEvent", LocalDateTime.now()));

            List<AuditRecordEntity> filtered =
                    ProcessStep.ORDER_RECEIVED.filterEvents(auditRecords);

            assertEquals(1, filtered.size());
            assertEquals("NewOrderObservedEvent", filtered.get(0).getEventName());
        }

        @Test
        void shouldFilterSuccessOrFailureEventsForInventoryReserved() {
            List<AuditRecordEntity> auditRecords = new ArrayList<>();
            auditRecords.add(createAuditRecord("OrderScheduledEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("InventoryReservedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("ReservationFailedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("OrderReservedEvent", LocalDateTime.now()));

            List<AuditRecordEntity> filtered =
                    ProcessStep.INVENTORY_RESERVED.filterEvents(auditRecords);

            assertEquals(2, filtered.size());
            assertTrue(
                    filtered.stream()
                            .anyMatch(r -> r.getEventName().equals("InventoryReservedEvent")));
            assertTrue(
                    filtered.stream()
                            .anyMatch(r -> r.getEventName().equals("ReservationFailedEvent")));
        }

        @Test
        void shouldFilterPickingCompletedEvents() {
            List<AuditRecordEntity> auditRecords = new ArrayList<>();
            auditRecords.add(createAuditRecord("PickingTaskCreatedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskSubmittedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskCompletedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskFailedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskCanceledEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("ReservationConsumedEvent", LocalDateTime.now()));

            List<AuditRecordEntity> filtered =
                    ProcessStep.PICKING_TASK_SUBMITTED.filterEvents(auditRecords);

            assertEquals(3, filtered.size());
            assertTrue(
                    filtered.stream()
                            .anyMatch(r -> r.getEventName().equals("PickingTaskCompletedEvent")));
            assertTrue(
                    filtered.stream()
                            .anyMatch(r -> r.getEventName().equals("PickingTaskFailedEvent")));
            assertTrue(
                    filtered.stream()
                            .anyMatch(r -> r.getEventName().equals("PickingTaskCanceledEvent")));
        }

        @Test
        void shouldReturnEmptyListWhenNoMatchingEvents() {
            List<AuditRecordEntity> auditRecords = new ArrayList<>();
            auditRecords.add(createAuditRecord("OrderScheduledEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("InventoryReservedEvent", LocalDateTime.now()));

            List<AuditRecordEntity> filtered =
                    ProcessStep.PICKING_COMPLETED.filterEvents(auditRecords);

            assertTrue(filtered.isEmpty());
        }
    }

    @Nested
    class CreateStepDtoTest {

        @Test
        void shouldCreatePendingDtoWhenNoEvents() {
            List<AuditRecordEntity> emptyList = new ArrayList<>();

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.ORDER_SCHEDULED.createStepDto(emptyList);

            assertEquals(2, dto.getStepNumber());
            assertEquals("Order Scheduled", dto.getStepName());
            assertEquals("PENDING", dto.getStatus());
            assertNull(dto.getTimestamp());
        }

        @Test
        void shouldCreateSuccessDtoForSingleEvent() {
            LocalDateTime now = LocalDateTime.now();
            List<AuditRecordEntity> events = new ArrayList<>();
            events.add(createAuditRecord("OrderScheduledEvent", now));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.ORDER_SCHEDULED.createStepDto(events);

            assertEquals(2, dto.getStepNumber());
            assertEquals("Order Scheduled", dto.getStepName());
            assertEquals("SUCCESS", dto.getStatus());
            assertEquals(now, dto.getTimestamp());
        }

        @Test
        void shouldCreateSuccessDtoWhenSuccessEventPresent() {
            LocalDateTime successTime = LocalDateTime.now();
            List<AuditRecordEntity> events = new ArrayList<>();
            events.add(createAuditRecord("InventoryReservedEvent", successTime));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.INVENTORY_RESERVED.createStepDto(events);

            assertEquals(4, dto.getStepNumber());
            assertEquals("Inventory Reserved", dto.getStepName());
            assertEquals("SUCCESS", dto.getStatus());
            assertEquals(successTime, dto.getTimestamp());
        }

        @Test
        void shouldCreateFailedDtoWhenFailureEventPresent() {
            LocalDateTime failureTime = LocalDateTime.now();
            List<AuditRecordEntity> events = new ArrayList<>();
            events.add(createAuditRecord("ReservationFailedEvent", failureTime));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.INVENTORY_RESERVED.createStepDto(events);

            assertEquals(4, dto.getStepNumber());
            assertEquals("Inventory Reserved", dto.getStepName());
            assertEquals("FAILED", dto.getStatus());
            assertEquals(failureTime, dto.getTimestamp());
        }

        @Test
        void shouldCreateFailedDtoWhenPickingCompletedFail() {
            List<AuditRecordEntity> auditRecords = new ArrayList<>();
            auditRecords.add(createAuditRecord("PickingTaskCreatedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskSubmittedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskCompletedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskFailedEvent", LocalDateTime.now()));
            auditRecords.add(createAuditRecord("PickingTaskCanceledEvent", LocalDateTime.now()));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.PICKING_TASK_SUBMITTED.createStepDto(auditRecords);

            assertEquals("FAILED", dto.getStatus());
        }

        @Test
        void shouldUseLatestEventTimestamp() {
            LocalDateTime earlier = LocalDateTime.now().minusHours(2);
            LocalDateTime later = LocalDateTime.now();
            List<AuditRecordEntity> events = new ArrayList<>();
            events.add(createAuditRecord("OrderScheduledEvent", earlier));
            events.add(createAuditRecord("OrderScheduledEvent", later));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.ORDER_SCHEDULED.createStepDto(events);

            assertEquals("SUCCESS", dto.getStatus());
            assertEquals(later, dto.getTimestamp());
        }

        @Test
        void shouldPrioritizeFailureOverSuccessWhenBothPresent() {
            LocalDateTime successTime = LocalDateTime.now().minusHours(1);
            LocalDateTime failureTime = LocalDateTime.now();
            List<AuditRecordEntity> events = new ArrayList<>();
            events.add(createAuditRecord("InventoryReservedEvent", successTime));
            events.add(createAuditRecord("ReservationFailedEvent", failureTime));

            OrderProcessStatusDto.ProcessStepDto dto =
                    ProcessStep.INVENTORY_RESERVED.createStepDto(events);

            assertEquals("FAILED", dto.getStatus());
            assertEquals(failureTime, dto.getTimestamp());
        }
    }

    private AuditRecordEntity createAuditRecord(String eventName, LocalDateTime timestamp) {
        AuditRecordEntity record = new AuditRecordEntity();
        record.setRecordId(UUID.randomUUID().toString());
        record.setAggregateType("Order");
        record.setAggregateId("ORDER-001");
        record.setEventName(eventName);
        record.setEventTimestamp(timestamp);
        record.setEventMetadata("{}");
        record.setPayload("{}");
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
