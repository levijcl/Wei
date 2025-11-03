package com.wei.orchestrator.integration.wes.query;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import com.wei.orchestrator.wes.query.PickingTaskQueryService;
import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PickingTaskQueryServiceIntegrationTest {

    @Autowired private PickingTaskQueryService pickingTaskQueryService;

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @Nested
    class getPickingTask {

        @Test
        void shouldRetrieveTaskByIdWithAllDetails() {
            List<TaskItem> items =
                    List.of(
                            TaskItem.of("SKU_QUERY_001", 10, "A-01-01"),
                            TaskItem.of("SKU_QUERY_002", 5, "A-01-02"));

            PickingTask task = PickingTask.createForOrder("ORDER_QUERY_DETAIL_001", items, 7);
            task.submitToWes(WesTaskId.of("WES_QUERY_001"));
            PickingTask savedTask = pickingTaskRepository.save(task);

            PickingTaskDetailDto result =
                    pickingTaskQueryService.getPickingTask(savedTask.getTaskId());

            assertNotNull(result);
            assertEquals(savedTask.getTaskId(), result.getTaskId());
            assertEquals("WES_QUERY_001", result.getWesTaskId());
            assertEquals("ORDER_QUERY_DETAIL_001", result.getOrderId());
            assertEquals(TaskStatus.SUBMITTED, result.getStatus());
            assertEquals(7, result.getPriority());
            assertEquals(2, result.getItems().size());
            assertEquals("SKU_QUERY_001", result.getItems().get(0).getSku());
            assertEquals(10, result.getItems().get(0).getQuantity());
        }

        @Test
        void shouldHandleCompletedTask() {
            List<TaskItem> items = List.of(TaskItem.of("SKU_COMPLETED_001", 15, "B-02-01"));

            PickingTask task = PickingTask.createForOrder("ORDER_COMPLETED_001", items, 3);
            task.submitToWes(WesTaskId.of("WES_COMPLETED_001"));
            task.updateStatusFromWes(TaskStatus.IN_PROGRESS);
            task.markCompleted();
            PickingTask savedTask = pickingTaskRepository.save(task);

            PickingTaskDetailDto result =
                    pickingTaskQueryService.getPickingTask(savedTask.getTaskId());

            assertEquals(TaskStatus.COMPLETED, result.getStatus());
            assertNotNull(result.getCompletedAt());
        }

        @Test
        void shouldHandleCanceledTask() {
            List<TaskItem> items = List.of(TaskItem.of("SKU_CANCELED_001", 20, "C-03-01"));

            PickingTask task = PickingTask.createForOrder("ORDER_CANCELED_001", items, 5);
            task.submitToWes(WesTaskId.of("WES_CANCELED_001"));
            task.cancel("Customer requested cancellation");
            PickingTask savedTask = pickingTaskRepository.save(task);

            PickingTaskDetailDto result =
                    pickingTaskQueryService.getPickingTask(savedTask.getTaskId());

            assertEquals(TaskStatus.CANCELED, result.getStatus());
            assertEquals("Customer requested cancellation", result.getFailureReason());
            assertNotNull(result.getCanceledAt());
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskQueryService.getPickingTask("NON_EXISTENT_TASK"));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }
    }

    @Nested
    class getPickingTasksByOrderId {

        @Test
        void shouldRetrieveAllTasksForOrder() {
            List<TaskItem> items1 = List.of(TaskItem.of("SKU_LIST_001", 8, "D-04-01"));
            PickingTask task1 = PickingTask.createForOrder("ORDER_LIST_001", items1, 4);
            task1.submitToWes(WesTaskId.of("WES_LIST_001"));
            pickingTaskRepository.save(task1);

            List<TaskItem> items2 =
                    List.of(
                            TaskItem.of("SKU_LIST_002", 12, "D-04-02"),
                            TaskItem.of("SKU_LIST_003", 6, "D-04-03"));
            PickingTask task2 = PickingTask.createForOrder("ORDER_LIST_001", items2, 6);
            task2.submitToWes(WesTaskId.of("WES_LIST_002"));
            pickingTaskRepository.save(task2);

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_LIST_001");

            assertEquals(2, results.size());
            assertTrue(results.stream().allMatch(t -> "ORDER_LIST_001".equals(t.getOrderId())));
            assertTrue(results.stream().anyMatch(t -> t.getItemCount() == 1));
            assertTrue(results.stream().anyMatch(t -> t.getItemCount() == 2));
        }

        @Test
        void shouldReturnEmptyListForNonExistentOrder() {
            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_NOT_EXISTS");

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        void shouldCalculateItemCountCorrectly() {
            List<TaskItem> items =
                    List.of(
                            TaskItem.of("SKU_COUNT_001", 5, "E-05-01"),
                            TaskItem.of("SKU_COUNT_002", 10, "E-05-02"),
                            TaskItem.of("SKU_COUNT_003", 15, "E-05-03"),
                            TaskItem.of("SKU_COUNT_004", 20, "E-05-04"));

            PickingTask task = PickingTask.createForOrder("ORDER_COUNT_001", items, 9);
            pickingTaskRepository.save(task);

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_COUNT_001");

            assertEquals(1, results.size());
            assertEquals(4, results.get(0).getItemCount());
        }

        @Test
        void shouldIncludeTasksWithDifferentStatuses() {
            List<TaskItem> items1 = List.of(TaskItem.of("SKU_STATUS_001", 3, "F-06-01"));
            PickingTask task1 = PickingTask.createForOrder("ORDER_STATUS_001", items1, 2);
            pickingTaskRepository.save(task1);

            List<TaskItem> items2 = List.of(TaskItem.of("SKU_STATUS_002", 7, "F-06-02"));
            PickingTask task2 = PickingTask.createForOrder("ORDER_STATUS_001", items2, 8);
            task2.submitToWes(WesTaskId.of("WES_STATUS_002"));
            pickingTaskRepository.save(task2);

            List<TaskItem> items3 = List.of(TaskItem.of("SKU_STATUS_003", 11, "F-06-03"));
            PickingTask task3 = PickingTask.createForOrder("ORDER_STATUS_001", items3, 5);
            task3.submitToWes(WesTaskId.of("WES_STATUS_003"));
            task3.markCompleted();
            pickingTaskRepository.save(task3);

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_STATUS_001");

            assertEquals(3, results.size());
            assertTrue(results.stream().anyMatch(t -> t.getStatus() == TaskStatus.PENDING));
            assertTrue(results.stream().anyMatch(t -> t.getStatus() == TaskStatus.SUBMITTED));
            assertTrue(results.stream().anyMatch(t -> t.getStatus() == TaskStatus.COMPLETED));
        }
    }
}
