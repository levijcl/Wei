package com.wei.orchestrator.integration.wes.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wei.orchestrator.inventory.domain.model.InventoryTransaction;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.domain.repository.InventoryTransactionRepository;
import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.AuditRecord;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.shared.domain.repository.AuditRecordRepository;
import com.wei.orchestrator.wes.application.eventhandler.WesTaskStatusUpdatedEventHandler;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class WesTaskStatusUpdatedEventHandlerIntegrationTest {

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @Autowired private WesTaskStatusUpdatedEventHandler eventHandler;

    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private AuditRecordRepository auditRecordRepository;

    @Autowired private OrderRepository orderRepository;

    @Autowired private InventoryTransactionRepository inventoryTransactionRepository;

    @MockitoBean private InventoryPort inventoryPort;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldMarkTaskAsCompletedWhenCompletedStatusReceived() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-001", 10, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.COMPLETED);

            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.COMPLETED, foundTask.get().getStatus());
        }

        @Test
        void shouldMarkTaskAsFailedWhenFailedStatusReceived() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-002", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.FAILED);

            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.FAILED, foundTask.get().getStatus());
            assertNotNull(foundTask.get().getFailureReason());
            assertTrue(foundTask.get().getFailureReason().contains("Failed in WES"));
        }

        @Test
        void shouldCancelTaskWhenCanceledStatusReceived() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-003", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.CANCELED);

            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.CANCELED, foundTask.get().getStatus());
            assertNotNull(foundTask.get().getFailureReason());
            assertTrue(foundTask.get().getFailureReason().contains("Canceled in WES"));
        }

        @Test
        void shouldUpdateStatusWhenInProgressReceived() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-004", 7, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.IN_PROGRESS);

            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, foundTask.get().getStatus());
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFound() {
            String taskId = "task_id";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(taskId, TaskStatus.COMPLETED);

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleWesTaskStatusUpdated(event);
                            });

            assertTrue(exception.getMessage().contains("PickingTask not found for taskId"));
        }

        @Test
        void shouldHandleMultipleStatusUpdatesForSameTask() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-005", 2, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event1 =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.IN_PROGRESS);
            eventHandler.handleWesTaskStatusUpdated(event1);

            Optional<PickingTask> foundTask1 =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask1.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, foundTask1.get().getStatus());

            WesTaskStatusUpdatedEvent event2 =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.COMPLETED);
            eventHandler.handleWesTaskStatusUpdated(event2);

            Optional<PickingTask> foundTask2 =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask2.isPresent());
            assertEquals(TaskStatus.COMPLETED, foundTask2.get().getStatus());
        }

        @Test
        void shouldHandleCompletedStatusForSubmittedTask() {
            String wesTaskId = "WES-INT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-006", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            assertEquals(TaskStatus.SUBMITTED, pickingTask.getStatus());

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.COMPLETED);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.COMPLETED, foundTask.get().getStatus());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldCommitTaskUpdateWhenHandlerSucceeds() {
            String wesTaskId = "WES-SUCCESS-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<TaskItem> items = List.of(TaskItem.of("SKU-100", 1, "WH-001"));
                        PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
                        pickingTask.setTaskId("task_001");
                        pickingTask.submitToWes(WesTaskId.of(wesTaskId));
                        pickingTaskRepository.save(pickingTask);
                        return null;
                    });

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent("task_001", TaskStatus.COMPLETED);

            eventHandler.handleWesTaskStatusUpdated(event);

            List<PickingTask> tasks = pickingTaskRepository.findByWesTaskId(wesTaskId);
            assertFalse(tasks.isEmpty());
            assertEquals(TaskStatus.COMPLETED, tasks.get(0).getStatus());
        }

        @Test
        void shouldRollbackWhenHandlerFails() {
            String wesTaskId = "WES-FAIL-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "FAIL-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<TaskItem> items = List.of(TaskItem.of("SKU-200", 2, "WH-001"));
                        PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
                        pickingTask.submitToWes(WesTaskId.of(wesTaskId));
                        pickingTaskRepository.save(pickingTask);
                        return null;
                    });

            String nonExistentTaskId = "TASK-NON-EXISTENT";
            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(nonExistentTaskId, TaskStatus.COMPLETED);

            assertThrows(
                    IllegalStateException.class,
                    () -> {
                        eventHandler.handleWesTaskStatusUpdated(event);
                    });

            List<PickingTask> tasks = pickingTaskRepository.findByWesTaskId(wesTaskId);
            assertFalse(tasks.isEmpty());
            assertEquals(
                    TaskStatus.SUBMITTED,
                    tasks.get(0).getStatus(),
                    "Task status should remain SUBMITTED when handler fails");
        }
    }

    @Nested
    class StatusRoutingBehavior {

        @Test
        void shouldRouteCompletedStatusToMarkTaskCompleted() {
            String wesTaskId = "WES-ROUTE-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "ROUTE-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-300", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.COMPLETED);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.COMPLETED, foundTask.get().getStatus());
            assertNull(foundTask.get().getFailureReason());
        }

        @Test
        void shouldRouteFailedStatusToMarkTaskFailed() {
            String wesTaskId = "WES-ROUTE-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "ROUTE-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-301", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.FAILED);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.FAILED, foundTask.get().getStatus());
            assertEquals("Failed in WES", foundTask.get().getFailureReason());
        }

        @Test
        void shouldRouteCanceledStatusToCancelTask() {
            String wesTaskId = "WES-ROUTE-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "ROUTE-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-302", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.CANCELED);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.CANCELED, foundTask.get().getStatus());
            assertEquals("Canceled in WES", foundTask.get().getFailureReason());
        }

        @Test
        void shouldRouteInProgressStatusToUpdateTaskStatusFromWes() {
            String wesTaskId = "WES-ROUTE-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "ROUTE-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-303", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.IN_PROGRESS);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, foundTask.get().getStatus());
            assertNull(foundTask.get().getFailureReason());
            assertNull(foundTask.get().getFailureReason());
        }
    }

    @Nested
    class IdempotencyScenarios {

        @Test
        void shouldBeIdempotentWhenCompletedStatusProcessedMultipleTimes() {
            String wesTaskId = "WES-IDEMPOTENT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "IDEMPOTENT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-400", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.COMPLETED);

            eventHandler.handleWesTaskStatusUpdated(event);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.COMPLETED, foundTask.get().getStatus());
        }

        @Test
        void shouldBeIdempotentWhenFailedStatusProcessedMultipleTimes() {
            String wesTaskId = "WES-IDEMPOTENT-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "IDEMPOTENT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<TaskItem> items = List.of(TaskItem.of("SKU-401", 1, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTask = pickingTaskRepository.save(pickingTask);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(pickingTask.getTaskId(), TaskStatus.FAILED);

            eventHandler.handleWesTaskStatusUpdated(event);
            eventHandler.handleWesTaskStatusUpdated(event);

            Optional<PickingTask> foundTask =
                    pickingTaskRepository.findById(pickingTask.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.FAILED, foundTask.get().getStatus());
            assertEquals("Failed in WES", foundTask.get().getFailureReason());
        }
    }

    @Nested
    class EventCorrelation {

        @Test
        void shouldRecordEventsWithSameCorrelationIdWhenTaskCompleted() {
            String wesTaskId = "WES-CORR-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "CORR-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();
            String sku = "SKU-CORR-001";

            Order order =
                    new Order(orderId, List.of(new OrderLineItem(sku, 10, BigDecimal.TEN)));
            order.createOrder();
            order.markReadyForFulfillment();
            order.reserveLineItem(
                    order.getOrderLineItems().get(0).getLineItemId(),
                    "TX-001",
                    "EXT-001",
                    "WH-001");
            order.markItemsAsPickingInProgress(List.of(sku), wesTaskId);
            orderRepository.save(order);

            InventoryTransaction transaction =
                    InventoryTransaction.createReservation(orderId, sku, "WH-001", 10);
            transaction.markAsReserved(ExternalReservationId.of("EXT-001"));
            inventoryTransactionRepository.save(transaction);

            List<TaskItem> items = List.of(TaskItem.of(sku, 10, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTaskRepository.save(pickingTask);

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:WesObserver", correlationId, null);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(
                            pickingTask.getTaskId(), TaskStatus.COMPLETED, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");
            assertEquals(3, auditRecords.size(), "Should have exactly 3 audit records");

            boolean allHaveSameCorrelationId =
                    auditRecords.stream()
                            .allMatch(
                                    record ->
                                            correlationId.equals(
                                                    record.getEventMetadata().getCorrelationId()));
            assertTrue(
                    allHaveSameCorrelationId,
                    "All audit records should share the same correlationId");

            List<String> eventNames = auditRecords.stream().map(AuditRecord::getEventName).toList();
            assertTrue(
                    eventNames.contains("WesTaskStatusUpdatedEvent"),
                    "Should audit WesTaskStatusUpdatedEvent");
            assertTrue(
                    eventNames.contains("PickingTaskCompletedEvent"),
                    "Should audit PickingTaskCompletedEvent");
            assertTrue(
                    eventNames.contains("ReservationConsumedEvent"),
                    "Should audit ReservationConsumedEvent");

            AuditRecord wesTaskStatusUpdatedRecord =
                    auditRecords.stream()
                            .filter(r -> "WesTaskStatusUpdatedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:WesObserver",
                    wesTaskStatusUpdatedRecord.getEventMetadata().getTriggerSource(),
                    "WesTaskStatusUpdatedEvent should have trigger source from scheduler");

            AuditRecord pickingTaskCompletedRecord =
                    auditRecords.stream()
                            .filter(r -> "PickingTaskCompletedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "WesTaskStatusUpdatedEvent",
                    pickingTaskCompletedRecord.getEventMetadata().getTriggerSource(),
                    "PickingTaskCompletedEvent should have trigger source from"
                            + " WesTaskStatusUpdatedEvent");

            AuditRecord reservationConsumedRecord =
                    auditRecords.stream()
                            .filter(r -> "ReservationConsumedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "PickingTaskCompletedEvent",
                    reservationConsumedRecord.getEventMetadata().getTriggerSource(),
                    "ReservationConsumedEvent should have trigger source from"
                            + " PickingTaskCompletedEvent");
        }

        @Test
        void shouldRecordEventsWithSameCorrelationIdWhenTaskFailed() {
            String wesTaskId = "WES-CORR-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "CORR-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            List<TaskItem> items = List.of(TaskItem.of("SKU-CORR-002", 5, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTaskRepository.save(pickingTask);

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:WesObserver", correlationId, null);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(
                            pickingTask.getTaskId(), TaskStatus.FAILED, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");
            assertEquals(2, auditRecords.size(), "Should have exactly 2 audit records");

            boolean allHaveSameCorrelationId =
                    auditRecords.stream()
                            .allMatch(
                                    record ->
                                            correlationId.equals(
                                                    record.getEventMetadata().getCorrelationId()));
            assertTrue(
                    allHaveSameCorrelationId,
                    "All audit records should share the same correlationId");

            List<String> eventNames = auditRecords.stream().map(AuditRecord::getEventName).toList();
            assertTrue(
                    eventNames.contains("WesTaskStatusUpdatedEvent"),
                    "Should audit WesTaskStatusUpdatedEvent");
            assertTrue(
                    eventNames.contains("PickingTaskFailedEvent"),
                    "Should audit PickingTaskFailedEvent");

            AuditRecord wesTaskStatusUpdatedRecord =
                    auditRecords.stream()
                            .filter(r -> "WesTaskStatusUpdatedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:WesObserver",
                    wesTaskStatusUpdatedRecord.getEventMetadata().getTriggerSource(),
                    "WesTaskStatusUpdatedEvent should have trigger source from scheduler");

            AuditRecord pickingTaskFailedRecord =
                    auditRecords.stream()
                            .filter(r -> "PickingTaskFailedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "WesTaskStatusUpdatedEvent",
                    pickingTaskFailedRecord.getEventMetadata().getTriggerSource(),
                    "PickingTaskFailedEvent should have trigger source from"
                            + " WesTaskStatusUpdatedEvent");
        }

        @Test
        void shouldRecordEventsWithSameCorrelationIdWhenTaskCanceled() {
            String wesTaskId = "WES-CORR-" + UUID.randomUUID().toString().substring(0, 8);
            String orderId = "CORR-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            UUID correlationId = UUID.randomUUID();

            List<TaskItem> items = List.of(TaskItem.of("SKU-CORR-003", 3, "WH-001"));
            PickingTask pickingTask = PickingTask.createForOrder(orderId, items, 5);
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));
            pickingTaskRepository.save(pickingTask);

            TriggerContext triggerContext =
                    TriggerContext.of("Scheduled:WesObserver", correlationId, null);

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(
                            pickingTask.getTaskId(), TaskStatus.CANCELED, triggerContext);

            transactionTemplate.execute(
                    status -> {
                        eventPublisher.publishEvent(event);
                        return null;
                    });

            List<AuditRecord> auditRecords =
                    auditRecordRepository.findByCorrelationId(correlationId);

            assertFalse(auditRecords.isEmpty(), "Should have audit records");
            assertEquals(2, auditRecords.size(), "Should have exactly 2 audit records");

            boolean allHaveSameCorrelationId =
                    auditRecords.stream()
                            .allMatch(
                                    record ->
                                            correlationId.equals(
                                                    record.getEventMetadata().getCorrelationId()));
            assertTrue(
                    allHaveSameCorrelationId,
                    "All audit records should share the same correlationId");

            List<String> eventNames = auditRecords.stream().map(AuditRecord::getEventName).toList();
            assertTrue(
                    eventNames.contains("WesTaskStatusUpdatedEvent"),
                    "Should audit WesTaskStatusUpdatedEvent");
            assertTrue(
                    eventNames.contains("PickingTaskCanceledEvent"),
                    "Should audit PickingTaskCanceledEvent");

            AuditRecord wesTaskStatusUpdatedRecord =
                    auditRecords.stream()
                            .filter(r -> "WesTaskStatusUpdatedEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "Scheduled:WesObserver",
                    wesTaskStatusUpdatedRecord.getEventMetadata().getTriggerSource(),
                    "WesTaskStatusUpdatedEvent should have trigger source from scheduler");

            AuditRecord pickingTaskCanceledRecord =
                    auditRecords.stream()
                            .filter(r -> "PickingTaskCanceledEvent".equals(r.getEventName()))
                            .findFirst()
                            .orElseThrow();
            assertEquals(
                    "WesTaskStatusUpdatedEvent",
                    pickingTaskCanceledRecord.getEventMetadata().getTriggerSource(),
                    "PickingTaskCanceledEvent should have trigger source from"
                            + " WesTaskStatusUpdatedEvent");
        }
    }
}
