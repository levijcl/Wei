package com.wei.orchestrator.unit.wes.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.query.PickingTaskQueryServiceImpl;
import com.wei.orchestrator.wes.query.dto.PickingTaskDetailDto;
import com.wei.orchestrator.wes.query.dto.PickingTaskSummaryDto;
import com.wei.orchestrator.wes.query.infrastructure.PickingTaskQueryRepository;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickingTaskQueryServiceTest {

    @Mock private PickingTaskQueryRepository pickingTaskQueryRepository;

    @InjectMocks private PickingTaskQueryServiceImpl pickingTaskQueryService;

    @Nested
    class getPickingTaskTest {
        @Test
        void shouldGetPickingTaskSuccessfully() {
            LocalDateTime createdAt = LocalDateTime.now();
            Object[] row =
                    new Object[] {
                        "TASK_001",
                        "WES_TASK_001",
                        "ORDER_001",
                        "ORCHESTRATOR_SUBMITTED",
                        5,
                        "SUBMITTED",
                        Timestamp.valueOf(createdAt),
                        null,
                        null,
                        null,
                        "SKU001",
                        10,
                        "WH001"
                    };

            when(pickingTaskQueryRepository.findTaskDetailById("TASK_001"))
                    .thenReturn((new ArrayList<Object[]>(Collections.singleton(row))));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_001");

            assertNotNull(result);
            assertEquals("TASK_001", result.getTaskId());
            assertEquals("WES_TASK_001", result.getWesTaskId());
            assertEquals("ORDER_001", result.getOrderId());
            assertEquals(TaskStatus.SUBMITTED, result.getStatus());
            assertEquals(5, result.getPriority());
            assertEquals(1, result.getItems().size());
            assertEquals("SKU001", result.getItems().get(0).getSku());
            assertEquals(10, result.getItems().get(0).getQuantity());
            assertEquals("WH001", result.getItems().get(0).getLocation());
            verify(pickingTaskQueryRepository).findTaskDetailById("TASK_001");
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskQueryRepository.findTaskDetailById("NON_EXISTENT"))
                    .thenReturn(List.of());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskQueryService.getPickingTask("NON_EXISTENT"));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldMapTaskItemsCorrectly() {
            LocalDateTime createdAt = LocalDateTime.now();
            Object[] row1 =
                    new Object[] {
                        "TASK_002",
                        "WES_TASK_002",
                        "ORDER_002",
                        "ORCHESTRATOR_SUBMITTED",
                        3,
                        "PENDING",
                        Timestamp.valueOf(createdAt),
                        null,
                        null,
                        null,
                        "SKU100",
                        5,
                        "A-01-01"
                    };
            Object[] row2 =
                    new Object[] {
                        "TASK_002",
                        "WES_TASK_002",
                        "ORDER_002",
                        "ORCHESTRATOR_SUBMITTED",
                        3,
                        "PENDING",
                        Timestamp.valueOf(createdAt),
                        null,
                        null,
                        null,
                        "SKU101",
                        3,
                        "A-01-02"
                    };

            when(pickingTaskQueryRepository.findTaskDetailById("TASK_002"))
                    .thenReturn(List.of(row1, row2));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_002");

            assertEquals(2, result.getItems().size());
            assertEquals("SKU100", result.getItems().get(0).getSku());
            assertEquals(5, result.getItems().get(0).getQuantity());
            assertEquals("A-01-01", result.getItems().get(0).getLocation());
            assertEquals("SKU101", result.getItems().get(1).getSku());
            assertEquals(3, result.getItems().get(1).getQuantity());
            assertEquals("A-01-02", result.getItems().get(1).getLocation());
        }

        @Test
        void shouldHandleNullWesTaskId() {
            LocalDateTime createdAt = LocalDateTime.now();
            Object[] row =
                    new Object[] {
                        "TASK_003",
                        null,
                        "ORDER_003",
                        "ORCHESTRATOR_SUBMITTED",
                        2,
                        "PENDING",
                        Timestamp.valueOf(createdAt),
                        null,
                        null,
                        null,
                        "SKU200",
                        8,
                        "B-01-01"
                    };

            when(pickingTaskQueryRepository.findTaskDetailById("TASK_003"))
                    .thenReturn((new ArrayList<Object[]>(Collections.singleton(row))));

            PickingTaskDetailDto result = pickingTaskQueryService.getPickingTask("TASK_003");

            assertNull(result.getWesTaskId());
            assertEquals("TASK_003", result.getTaskId());
            assertEquals("ORDER_003", result.getOrderId());
        }
    }

    @Nested
    class getPickingTasksByOrderIdTest {
        @Test
        void shouldGetAllTasksByOrderId() {
            LocalDateTime createdAt1 = LocalDateTime.now().minusHours(2);
            LocalDateTime createdAt2 = LocalDateTime.now().minusHours(1);

            Object[] row1 =
                    new Object[] {
                        "TASK_001",
                        "WES_001",
                        "ORDER_001",
                        "ORCHESTRATOR_SUBMITTED",
                        3,
                        "PENDING",
                        Timestamp.valueOf(createdAt1),
                        null,
                        null,
                        BigInteger.valueOf(1)
                    };

            Object[] row2 =
                    new Object[] {
                        "TASK_002",
                        "WES_002",
                        "ORDER_001",
                        "ORCHESTRATOR_SUBMITTED",
                        5,
                        "SUBMITTED",
                        Timestamp.valueOf(createdAt2),
                        null,
                        null,
                        BigInteger.valueOf(2)
                    };

            when(pickingTaskQueryRepository.findTaskSummariesByOrderId("ORDER_001"))
                    .thenReturn(List.of(row1, row2));

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_001");

            assertNotNull(results);
            assertEquals(2, results.size());
            assertEquals("TASK_001", results.get(0).getTaskId());
            assertEquals("ORDER_001", results.get(0).getOrderId());
            assertEquals(1, results.get(0).getItemCount());
            assertEquals("TASK_002", results.get(1).getTaskId());
            assertEquals(2, results.get(1).getItemCount());
            verify(pickingTaskQueryRepository).findTaskSummariesByOrderId("ORDER_001");
        }

        @Test
        void shouldReturnEmptyListWhenNoTasksFound() {
            when(pickingTaskQueryRepository.findTaskSummariesByOrderId("ORDER_999"))
                    .thenReturn(List.of());

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_999");

            assertNotNull(results);
            assertTrue(results.isEmpty());
        }

        @Test
        void shouldMapItemCountCorrectly() {
            LocalDateTime createdAt = LocalDateTime.now();
            Object[] row =
                    new Object[] {
                        "TASK_100",
                        "WES_100",
                        "ORDER_100",
                        "ORCHESTRATOR_SUBMITTED",
                        4,
                        "PENDING",
                        Timestamp.valueOf(createdAt),
                        null,
                        null,
                        BigInteger.valueOf(3)
                    };

            when(pickingTaskQueryRepository.findTaskSummariesByOrderId("ORDER_100"))
                    .thenReturn((new ArrayList<Object[]>(Collections.singleton(row))));

            List<PickingTaskSummaryDto> results =
                    pickingTaskQueryService.getPickingTasksByOrderId("ORDER_100");

            assertEquals(1, results.size());
            assertEquals(3, results.get(0).getItemCount());
        }
    }
}
