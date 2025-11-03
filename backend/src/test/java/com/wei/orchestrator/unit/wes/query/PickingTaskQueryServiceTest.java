package com.wei.orchestrator.unit.wes.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import com.wei.orchestrator.wes.query.PickingTaskQueryServiceImpl;
import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickingTaskQueryServiceTest {

    @Mock private PickingTaskRepository pickingTaskRepository;

    @InjectMocks private PickingTaskQueryServiceImpl pickingTaskQueryService;

    @Nested
    class getPickingTaskTest {
        @Test
        void shouldGetPickingTaskSuccessfully() {
            PickingTask task = mock(PickingTask.class);
            when(task.getTaskId()).thenReturn("TASK_001");
            when(task.getWesTaskId()).thenReturn(WesTaskId.of("WES_TASK_001"));
            when(task.getOrderId()).thenReturn("ORDER_001");
            when(task.getStatus()).thenReturn(TaskStatus.SUBMITTED);
            when(task.getPriority()).thenReturn(5);
            when(task.getItems()).thenReturn(List.of(TaskItem.of("SKU001", 10, "WH001")));

            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_001");

            assertNotNull(result);
            assertEquals("TASK_001", result.getTaskId());
            assertEquals("WES_TASK_001", result.getWesTaskId());
            assertEquals("ORDER_001", result.getOrderId());
            assertEquals(TaskStatus.SUBMITTED, result.getStatus());
            assertEquals(1, result.getItems().size());
            verify(pickingTaskRepository).findById("TASK_001");
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskQueryService.getPickingTask("NON_EXISTENT"));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldMapTaskItemsCorrectly() {
            PickingTask task = mock(PickingTask.class);
            when(task.getTaskId()).thenReturn("TASK_002");
            when(task.getItems())
                    .thenReturn(
                            List.of(
                                    TaskItem.of("SKU100", 5, "A-01-01"),
                                    TaskItem.of("SKU101", 3, "A-01-02")));

            when(pickingTaskRepository.findById("TASK_002")).thenReturn(Optional.of(task));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_002");

            assertEquals(2, result.getItems().size());
            assertEquals("SKU100", result.getItems().get(0).getSku());
            assertEquals(5, result.getItems().get(0).getQuantity());
            assertEquals("A-01-01", result.getItems().get(0).getLocation());
        }

        @Test
        void shouldHandleNullWesTaskId() {
            PickingTask task = mock(PickingTask.class);
            when(task.getTaskId()).thenReturn("TASK_003");
            when(task.getWesTaskId()).thenReturn(null);
            when(task.getItems()).thenReturn(List.of(TaskItem.of("SKU200", 8, "B-01-01")));

            when(pickingTaskRepository.findById("TASK_003")).thenReturn(Optional.of(task));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_003");

            assertNull(result.getWesTaskId());
        }
    }

    @Nested
    class getPickingTasksByOrderIdTest {
        @Test
        void shouldGetAllTasksByOrderId() {
            PickingTask task1 = mock(PickingTask.class);
            when(task1.getTaskId()).thenReturn("TASK_001");
            when(task1.getOrderId()).thenReturn("ORDER_001");
            when(task1.getStatus()).thenReturn(TaskStatus.PENDING);
            when(task1.getItems()).thenReturn(List.of(TaskItem.of("SKU001", 10, "WH001")));

            PickingTask task2 = mock(PickingTask.class);
            when(task2.getTaskId()).thenReturn("TASK_002");
            when(task2.getOrderId()).thenReturn("ORDER_001");
            when(task2.getStatus()).thenReturn(TaskStatus.SUBMITTED);
            when(task2.getItems())
                    .thenReturn(
                            List.of(
                                    TaskItem.of("SKU002", 5, "WH002"),
                                    TaskItem.of("SKU003", 3, "WH003")));

            when(pickingTaskRepository.findByOrderId("ORDER_001"))
                    .thenReturn(List.of(task1, task2));

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_001");

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("TASK_001", results.get(0).getTaskId());
            assertEquals("ORDER_001", results.get(0).getOrderId());
            assertEquals(1, results.get(0).getItemCount());
            assertEquals("TASK_002", results.get(1).getTaskId());
            assertEquals(2, results.get(1).getItemCount());
            verify(pickingTaskRepository).findByOrderId("ORDER_001");
        }

        @Test
        void shouldReturnEmptyListWhenNoTasksFound() {
            when(pickingTaskRepository.findByOrderId("ORDER_999")).thenReturn(List.of());

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_999");

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        void shouldMapItemCountCorrectly() {
            PickingTask task = mock(PickingTask.class);
            when(task.getTaskId()).thenReturn("TASK_100");
            when(task.getOrderId()).thenReturn("ORDER_100");
            when(task.getItems())
                    .thenReturn(
                            List.of(
                                    TaskItem.of("SKU1", 1, "L1"),
                                    TaskItem.of("SKU2", 2, "L2"),
                                    TaskItem.of("SKU3", 3, "L3")));

            when(pickingTaskRepository.findByOrderId("ORDER_100")).thenReturn(List.of(task));

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_100");

            assertEquals(1, results.size());
            assertEquals(3, results.get(0).getItemCount());
        }
    }
}
