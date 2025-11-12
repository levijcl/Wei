package com.wei.orchestrator.unit.wes.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.domain.exception.WesOperationException;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.infrastructure.adapter.WesHttpAdapter;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesTaskDto;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class WesHttpAdapterTest {

    @Mock private RestTemplate restTemplate;

    private WesHttpAdapter wesHttpAdapter;

    private static final String WES_BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        wesHttpAdapter = new WesHttpAdapter(restTemplate, WES_BASE_URL);
    }

    @Nested
    class WesStatusToTaskStatusMappingTest {

        @Test
        void shouldMapWesPendingToSubmitted() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-001");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-001", "PENDING");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-001"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.SUBMITTED, result.get());
        }

        @Test
        void shouldMapWesInProgressToInProgress() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-002");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-002", "IN_PROGRESS");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-002"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, result.get());
        }

        @Test
        void shouldMapWesCompletedToCompleted() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-003");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-003", "COMPLETED");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-003"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.COMPLETED, result.get());
        }

        @Test
        void shouldMapWesFailedToFailed() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-004");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-004", "FAILED");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-004"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.FAILED, result.get());
        }

        @Test
        void shouldMapWesCancelledToCanceled() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-005");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-005", "CANCELLED");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-005"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.CANCELED, result.get());
        }

        @Test
        void shouldHandleLowercaseWesStatus() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-006");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-006", "pending");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-006"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.SUBMITTED, result.get());
        }

        @Test
        void shouldHandleMixedCaseWesStatus() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-007");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-007", "In_Progress");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-007"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, result.get());
        }

        @Test
        void shouldDefaultToPendingForUnknownWesStatus() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-008");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-008", "UNKNOWN_STATUS");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-008"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.PENDING, result.get());
        }

        @Test
        void shouldDefaultToPendingForNullWesStatus() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-009");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-009", null);

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-009"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.PENDING, result.get());
        }

        @Test
        void shouldHandleEmptyStringWesStatus() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-010");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-010", "");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-010"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(TaskStatus.PENDING, result.get());
        }
    }

    @Nested
    class GetTaskStatusTest {

        @Test
        void shouldReturnEmptyWhenResponseBodyIsNull() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-011");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-011"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertFalse(result.isPresent());
        }

        @Test
        void shouldThrowWesTaskNotFoundExceptionWhenTaskNotFound() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-999");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-TASK-999"), eq(WesTaskDto.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

            assertThrows(
                    WesOperationException.class, () -> wesHttpAdapter.getTaskStatus(wesTaskId));
        }
    }

    @Nested
    class StatusMappingRationaleTest {

        @Test
        void shouldExplainWhyWesPendingMapsToSubmitted() {
            WesTaskId wesTaskId = WesTaskId.of("WES-TASK-001");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-TASK-001", "PENDING");

            when(restTemplate.getForEntity(any(String.class), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(
                    TaskStatus.SUBMITTED,
                    result.get(),
                    "WES PENDING should map to SUBMITTED because the task is already submitted to"
                            + " WES");
        }

        @Test
        void shouldVerifyAllTerminalStatesArePreserved() {
            WesTaskId completedTaskId = WesTaskId.of("WES-COMPLETED");
            WesTaskId failedTaskId = WesTaskId.of("WES-FAILED");
            WesTaskId canceledTaskId = WesTaskId.of("WES-CANCELED");

            WesTaskDto completedDto = createWesTaskDto("WES-COMPLETED", "COMPLETED");
            WesTaskDto failedDto = createWesTaskDto("WES-FAILED", "FAILED");
            WesTaskDto canceledDto = createWesTaskDto("WES-CANCELED", "CANCELLED");

            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-COMPLETED"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(completedDto, HttpStatus.OK));
            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-FAILED"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(failedDto, HttpStatus.OK));
            when(restTemplate.getForEntity(
                            eq(WES_BASE_URL + "/api/tasks/WES-CANCELED"), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(canceledDto, HttpStatus.OK));

            assertEquals(TaskStatus.COMPLETED, wesHttpAdapter.getTaskStatus(completedTaskId).get());
            assertEquals(TaskStatus.FAILED, wesHttpAdapter.getTaskStatus(failedTaskId).get());
            assertEquals(TaskStatus.CANCELED, wesHttpAdapter.getTaskStatus(canceledTaskId).get());
        }

        @Test
        void shouldVerifyInProgressStatusFlowsThrough() {
            WesTaskId wesTaskId = WesTaskId.of("WES-IN-PROGRESS");
            WesTaskDto wesTaskDto = createWesTaskDto("WES-IN-PROGRESS", "IN_PROGRESS");

            when(restTemplate.getForEntity(any(String.class), eq(WesTaskDto.class)))
                    .thenReturn(new ResponseEntity<>(wesTaskDto, HttpStatus.OK));

            Optional<TaskStatus> result = wesHttpAdapter.getTaskStatus(wesTaskId);

            assertTrue(result.isPresent());
            assertEquals(
                    TaskStatus.IN_PROGRESS,
                    result.get(),
                    "WES IN_PROGRESS should map directly to IN_PROGRESS");
        }
    }

    private WesTaskDto createWesTaskDto(String taskId, String status) {
        WesTaskDto dto = new WesTaskDto();
        dto.setTaskId(taskId);
        dto.setStatus(status);
        dto.setTaskType("PICKING");
        dto.setOrderId("ORDER-001");
        dto.setWarehouseId("WH001");
        dto.setPriority(5);
        return dto;
    }
}
