package com.wei.orchestrator.unit.wes.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.wes.domain.event.*;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskOrigin;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PickingTaskTest {

    @Test
    void shouldCreatePickingTaskForOrder() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-001", 10, "A-01-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-001", items, 5);

        assertNotNull(task);
        assertNotNull(task.getTaskId());
        assertTrue(task.getTaskId().startsWith("PICK-"));
        assertEquals("ORDER-001", task.getOrderId());
        assertEquals(TaskOrigin.ORCHESTRATOR_SUBMITTED, task.getOrigin());
        assertEquals(5, task.getPriority());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals(1, task.getItems().size());
        assertNotNull(task.getCreatedAt());
        assertNull(task.getWesTaskId());
        assertNull(task.getSubmittedAt());
    }

    @Test
    void shouldThrowExceptionWhenCreatingTaskWithNullOrderId() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-001", 10, "A-01-01"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PickingTask.createForOrder(null, items, 5));

        assertTrue(exception.getMessage().contains("Order ID cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenCreatingTaskWithEmptyItems() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PickingTask.createForOrder("ORDER-001", new ArrayList<>(), 5));

        assertTrue(exception.getMessage().contains("Task items cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenCreatingTaskWithInvalidPriority() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-001", 10, "A-01-01"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> PickingTask.createForOrder("ORDER-001", items, 11));

        assertTrue(exception.getMessage().contains("Priority must be between 1 and 10"));
    }

    @Test
    void shouldCreatePickingTaskFromWesTask() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-002", 5, "B-02-02"));

        PickingTask task = PickingTask.createFromWesTask("WES-TASK-001", items, 3);

        assertNotNull(task);
        assertNotNull(task.getTaskId());
        assertEquals("WES-TASK-001", task.getWesTaskId().getValue());
        assertNull(task.getOrderId());
        assertEquals(TaskOrigin.WES_DIRECT, task.getOrigin());
        assertEquals(3, task.getPriority());
        assertEquals(TaskStatus.SUBMITTED, task.getStatus());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getSubmittedAt());
    }

    @Test
    void shouldSubmitTaskToWes() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-003", 15, "C-03-03"));
        PickingTask task = PickingTask.createForOrder("ORDER-002", items, 7);
        WesTaskId wesTaskId = WesTaskId.of("WES-TASK-002");

        task.submitToWes(wesTaskId);

        assertEquals(wesTaskId, task.getWesTaskId());
        assertEquals(TaskStatus.SUBMITTED, task.getStatus());
        assertNotNull(task.getSubmittedAt());
    }

    @Test
    void shouldThrowExceptionWhenSubmittingNonPendingTask() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-004", 8, "D-04-04"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-003", items, 5);
        WesTaskId wesTaskId = WesTaskId.of("WES-TASK-004");

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> task.submitToWes(wesTaskId));

        assertTrue(exception.getMessage().contains("Task cannot be submitted in status"));
    }

    @Test
    void shouldUpdateStatusFromWesToCompleted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-005", 12, "E-05-05"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-005", items, 4);

        task.updateStatusFromWes(TaskStatus.COMPLETED);

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldUpdateStatusFromWesToFailed() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-006", 6, "F-06-06"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-006", items, 2);

        task.updateStatusFromWes(TaskStatus.FAILED);

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldUpdateStatusFromWesToCanceled() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-007", 20, "G-07-07"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-007", items, 6);

        task.updateStatusFromWes(TaskStatus.CANCELED);

        assertEquals(TaskStatus.CANCELED, task.getStatus());
        assertNotNull(task.getCanceledAt());
    }

    @Test
    void shouldThrowExceptionWhenUpdatingFromWeswithInvalidStatus() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-008", 9, "H-08-08"));
        PickingTask task = PickingTask.createForOrder("ORDER-003", items, 5);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> task.updateStatusFromWes(TaskStatus.COMPLETED));

        assertTrue(exception.getMessage().contains("Cannot update status from WES"));
    }

    @Test
    void shouldAdjustPriority() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-009", 7, "I-09-09"));
        PickingTask task = PickingTask.createForOrder("ORDER-004", items, 3);

        task.adjustPriority(8);

        assertEquals(8, task.getPriority());
    }

    @Test
    void shouldNotAdjustPriorityWhenSameValue() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-010", 11, "J-10-10"));
        PickingTask task = PickingTask.createForOrder("ORDER-005", items, 5);

        task.adjustPriority(5);

        assertEquals(5, task.getPriority());
    }

    @Test
    void shouldMarkAsCompleted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-011", 4, "K-11-11"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-008", items, 7);

        task.markCompleted();

        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldThrowExceptionWhenMarkingCompletedTaskAsCompleted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-012", 13, "L-12-12"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-009", items, 9);
        task.markCompleted();

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> task.markCompleted());

        assertTrue(exception.getMessage().contains("Task is already in terminal status"));
    }

    @Test
    void shouldMarkAsFailed() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-013", 16, "M-13-13"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-010", items, 6);

        task.markFailed("Inventory not available");

        assertEquals(TaskStatus.FAILED, task.getStatus());
        assertEquals("Inventory not available", task.getFailureReason());
        assertNotNull(task.getCompletedAt());
    }

    @Test
    void shouldCancelTask() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-014", 18, "N-14-14"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-011", items, 4);

        task.cancel("Order canceled by customer");

        assertEquals(TaskStatus.CANCELED, task.getStatus());
        assertEquals("Order canceled by customer", task.getFailureReason());
        assertNotNull(task.getCanceledAt());
    }

    @Test
    void shouldThrowExceptionWhenCancelingCompletedTask() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-015", 22, "O-15-15"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-012", items, 8);
        task.markCompleted();

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> task.cancel("Too late to cancel"));

        assertTrue(exception.getMessage().contains("Task cannot be canceled in status"));
    }

    @Test
    void shouldCollectDomainEventWhenTaskCreated() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-016", 25, "P-16-16"));

        PickingTask task = PickingTask.createForOrder("ORDER-006", items, 10);

        List<Object> events = task.getDomainEvents();
        assertEquals(1, events.size());
        assertInstanceOf(PickingTaskCreatedEvent.class, events.get(0));
    }

    @Test
    void shouldCollectDomainEventWhenTaskSubmitted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-017", 14, "Q-17-17"));
        PickingTask task = PickingTask.createForOrder("ORDER-007", items, 6);

        task.submitToWes(WesTaskId.of("WES-TASK-013"));

        List<Object> events = task.getDomainEvents();
        assertEquals(2, events.size());
        assertInstanceOf(PickingTaskSubmittedEvent.class, events.get(1));
    }

    @Test
    void shouldCollectDomainEventWhenTaskCompleted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-018", 19, "R-18-18"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-014", items, 5);

        task.markCompleted();

        List<Object> events = task.getDomainEvents();
        assertEquals(2, events.size());
        assertInstanceOf(PickingTaskCompletedEvent.class, events.get(1));
    }

    @Test
    void shouldCollectDomainEventWhenTaskFailed() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-019", 21, "S-19-19"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-015", items, 7);

        task.markFailed("System error");

        List<Object> events = task.getDomainEvents();
        assertEquals(2, events.size());
        assertInstanceOf(PickingTaskFailedEvent.class, events.get(1));
    }

    @Test
    void shouldCollectDomainEventWhenTaskCanceled() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-020", 23, "T-20-20"));
        PickingTask task = PickingTask.createFromWesTask("WES-TASK-016", items, 3);

        task.cancel("Business decision");

        List<Object> events = task.getDomainEvents();
        assertEquals(2, events.size());
        assertInstanceOf(PickingTaskCanceledEvent.class, events.get(1));
    }

    @Test
    void shouldCollectDomainEventWhenPriorityAdjusted() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-021", 26, "U-21-21"));
        PickingTask task = PickingTask.createForOrder("ORDER-008", items, 4);

        task.adjustPriority(9);

        List<Object> events = task.getDomainEvents();
        assertEquals(2, events.size());
        assertInstanceOf(PickingTaskPriorityAdjustedEvent.class, events.get(1));
    }

    @Test
    void shouldClearDomainEvents() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-022", 27, "V-22-22"));
        PickingTask task = PickingTask.createForOrder("ORDER-009", items, 8);

        task.clearDomainEvents();

        assertTrue(task.getDomainEvents().isEmpty());
    }

    @Test
    void shouldReturnImmutableListOfDomainEvents() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-023", 28, "W-23-23"));
        PickingTask task = PickingTask.createForOrder("ORDER-010", items, 2);

        List<Object> events = task.getDomainEvents();

        assertThrows(UnsupportedOperationException.class, () -> events.add(new Object()));
    }

    @Test
    void shouldReturnImmutableListOfItems() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-024", 29, "X-24-24"));
        PickingTask task = PickingTask.createForOrder("ORDER-011", items, 1);

        List<TaskItem> retrievedItems = task.getItems();

        assertThrows(
                UnsupportedOperationException.class,
                () -> retrievedItems.add(TaskItem.of("SKU-025", 30, "Y-25-25")));
    }
}
