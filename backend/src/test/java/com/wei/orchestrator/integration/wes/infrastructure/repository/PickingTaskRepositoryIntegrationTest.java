package com.wei.orchestrator.integration.wes.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.infrastructure.repository.PickingTaskRepositoryImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(PickingTaskRepositoryImpl.class)
class PickingTaskRepositoryIntegrationTest {

    @Autowired private PickingTaskRepositoryImpl pickingTaskRepository;

    @Test
    void shouldSaveAndFindPickingTaskById() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-001", 10, "A-01-01"));
        items.add(TaskItem.of("SKU-002", 5, "A-01-02"));

        PickingTask task = PickingTask.createForOrder("ORDER-001", items, 5);
        PickingTask savedTask = pickingTaskRepository.save(task);

        assertNotNull(savedTask);
        assertEquals(task.getTaskId(), savedTask.getTaskId());
        assertEquals("ORDER-001", savedTask.getOrderId());
        assertEquals(TaskStatus.PENDING, savedTask.getStatus());
        assertEquals(2, savedTask.getItems().size());
    }

    @Test
    void shouldFindPickingTaskByIdAfterSaving() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-100", 3, "B-02-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-002", items, 7);
        PickingTask savedTask = pickingTaskRepository.save(task);

        Optional<PickingTask> foundTask = pickingTaskRepository.findById(savedTask.getTaskId());

        assertTrue(foundTask.isPresent());
        assertEquals(savedTask.getTaskId(), foundTask.get().getTaskId());
        assertEquals("ORDER-002", foundTask.get().getOrderId());
        assertEquals(1, foundTask.get().getItems().size());
        assertEquals("SKU-100", foundTask.get().getItems().get(0).getSku());
    }

    @Test
    void shouldReturnEmptyWhenPickingTaskNotFound() {
        Optional<PickingTask> foundTask = pickingTaskRepository.findById("NON-EXISTENT");

        assertFalse(foundTask.isPresent());
    }

    @Test
    void shouldSavePickingTaskWithWesTaskId() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-200", 7, "C-03-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-003", items, 3);
        task.submitToWes(WesTaskId.of("WES-TASK-001"));

        PickingTask savedTask = pickingTaskRepository.save(task);

        Optional<PickingTask> foundTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(foundTask.isPresent());
        assertEquals(TaskStatus.SUBMITTED, foundTask.get().getStatus());
        assertNotNull(foundTask.get().getWesTaskId());
        assertEquals("WES-TASK-001", foundTask.get().getWesTaskId().getValue());
    }

    @Test
    void shouldUpdateExistingPickingTask() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-300", 4, "D-04-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-004", items, 8);
        PickingTask savedTask = pickingTaskRepository.save(task);

        savedTask.submitToWes(WesTaskId.of("WES-TASK-002"));
        PickingTask updatedTask = pickingTaskRepository.save(savedTask);

        Optional<PickingTask> foundTask = pickingTaskRepository.findById(updatedTask.getTaskId());
        assertTrue(foundTask.isPresent());
        assertEquals(TaskStatus.SUBMITTED, foundTask.get().getStatus());
        assertEquals("WES-TASK-002", foundTask.get().getWesTaskId().getValue());
    }

    @Test
    void shouldFindPickingTasksByOrderId() {
        List<TaskItem> items1 = new ArrayList<>();
        items1.add(TaskItem.of("SKU-400", 8, "E-05-01"));
        PickingTask task1 = PickingTask.createForOrder("ORDER-005", items1, 6);
        pickingTaskRepository.save(task1);

        List<TaskItem> items2 = new ArrayList<>();
        items2.add(TaskItem.of("SKU-401", 9, "E-05-02"));
        PickingTask task2 = PickingTask.createForOrder("ORDER-005", items2, 4);
        pickingTaskRepository.save(task2);

        List<PickingTask> tasks = pickingTaskRepository.findByOrderId("ORDER-005");

        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().allMatch(t -> "ORDER-005".equals(t.getOrderId())));
    }

    @Test
    void shouldFindPickingTasksByStatus() {
        List<TaskItem> items1 = new ArrayList<>();
        items1.add(TaskItem.of("SKU-500", 12, "F-06-01"));
        PickingTask task1 = PickingTask.createForOrder("ORDER-006", items1, 2);
        pickingTaskRepository.save(task1);

        List<TaskItem> items2 = new ArrayList<>();
        items2.add(TaskItem.of("SKU-501", 15, "F-06-02"));
        PickingTask task2 = PickingTask.createForOrder("ORDER-007", items2, 9);
        task2.submitToWes(WesTaskId.of("WES-TASK-003"));
        pickingTaskRepository.save(task2);

        List<PickingTask> pendingTasks = pickingTaskRepository.findByStatus(TaskStatus.PENDING);
        List<PickingTask> submittedTasks = pickingTaskRepository.findByStatus(TaskStatus.SUBMITTED);

        assertFalse(pendingTasks.isEmpty());
        assertFalse(submittedTasks.isEmpty());
        assertTrue(pendingTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.PENDING));
        assertTrue(submittedTasks.stream().allMatch(t -> t.getStatus() == TaskStatus.SUBMITTED));
    }

    @Test
    void shouldFindPickingTasksByWesTaskId() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-600", 20, "G-07-01"));
        PickingTask task = PickingTask.createForOrder("ORDER-008", items, 10);
        task.submitToWes(WesTaskId.of("WES-TASK-UNIQUE-001"));
        pickingTaskRepository.save(task);

        List<PickingTask> tasks = pickingTaskRepository.findByWesTaskId("WES-TASK-UNIQUE-001");

        assertEquals(1, tasks.size());
        assertEquals("WES-TASK-UNIQUE-001", tasks.get(0).getWesTaskId().getValue());
    }

    @Test
    void shouldDeletePickingTaskById() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-700", 25, "H-08-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-009", items, 1);
        PickingTask savedTask = pickingTaskRepository.save(task);

        Optional<PickingTask> foundTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(foundTask.isPresent());

        pickingTaskRepository.deleteById(savedTask.getTaskId());

        Optional<PickingTask> deletedTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertFalse(deletedTask.isPresent());
    }

    @Test
    void shouldCascadeDeleteTaskItems() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-800", 1, "I-09-01"));
        items.add(TaskItem.of("SKU-801", 2, "I-09-02"));
        items.add(TaskItem.of("SKU-802", 3, "I-09-03"));

        PickingTask task = PickingTask.createForOrder("ORDER-010", items, 5);
        PickingTask savedTask = pickingTaskRepository.save(task);

        Optional<PickingTask> foundTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(foundTask.isPresent());
        assertEquals(3, foundTask.get().getItems().size());

        pickingTaskRepository.deleteById(savedTask.getTaskId());

        Optional<PickingTask> deletedTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertFalse(deletedTask.isPresent());
    }

    @Test
    void shouldSaveCompletePickingTaskLifecycle() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-900", 30, "J-10-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-011", items, 7);
        PickingTask savedTask = pickingTaskRepository.save(task);

        Optional<PickingTask> createdTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(createdTask.isPresent());
        assertEquals(TaskStatus.PENDING, createdTask.get().getStatus());

        PickingTask taskToSubmit = createdTask.get();
        taskToSubmit.submitToWes(WesTaskId.of("WES-TASK-LIFECYCLE"));
        pickingTaskRepository.save(taskToSubmit);

        Optional<PickingTask> submittedTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(submittedTask.isPresent());
        assertEquals(TaskStatus.SUBMITTED, submittedTask.get().getStatus());

        PickingTask taskToComplete = submittedTask.get();
        taskToComplete.updateStatusFromWes(TaskStatus.IN_PROGRESS);
        pickingTaskRepository.save(taskToComplete);

        Optional<PickingTask> inProgressTask =
                pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(inProgressTask.isPresent());
        assertEquals(TaskStatus.IN_PROGRESS, inProgressTask.get().getStatus());

        PickingTask taskToFinish = inProgressTask.get();
        taskToFinish.updateStatusFromWes(TaskStatus.COMPLETED);
        pickingTaskRepository.save(taskToFinish);

        Optional<PickingTask> completedTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(completedTask.isPresent());
        assertEquals(TaskStatus.COMPLETED, completedTask.get().getStatus());
        assertNotNull(completedTask.get().getCompletedAt());
    }

    @Test
    void shouldSaveAndRetrieveTaskWithCanceledStatus() {
        List<TaskItem> items = new ArrayList<>();
        items.add(TaskItem.of("SKU-950", 18, "K-11-01"));

        PickingTask task = PickingTask.createForOrder("ORDER-012", items, 6);
        task.submitToWes(WesTaskId.of("WES-TASK-CANCEL-TEST"));
        PickingTask savedTask = pickingTaskRepository.save(task);

        savedTask.cancel("Customer requested cancellation");
        pickingTaskRepository.save(savedTask);

        Optional<PickingTask> canceledTask = pickingTaskRepository.findById(savedTask.getTaskId());
        assertTrue(canceledTask.isPresent());
        assertEquals(TaskStatus.CANCELED, canceledTask.get().getStatus());
        assertEquals("Customer requested cancellation", canceledTask.get().getFailureReason());
        assertNotNull(canceledTask.get().getCanceledAt());
    }
}
