package com.wei.orchestrator.integration.wes.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.*;
import com.wei.orchestrator.wes.application.command.dto.TaskItemDto;
import com.wei.orchestrator.wes.application.dto.WesOperationResultDto;
import com.wei.orchestrator.wes.domain.exception.WesPriorityUpdateException;
import com.wei.orchestrator.wes.domain.exception.WesSubmissionException;
import com.wei.orchestrator.wes.domain.exception.WesTaskCancellationException;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PickingTaskApplicationServiceIntegrationTest {

    @Autowired private PickingTaskApplicationService pickingTaskApplicationService;

    @Autowired private PickingTaskRepository pickingTaskRepository;

    @MockitoBean private WesPort wesPort;

    @Nested
    class createPickingTaskForOrder {

        @Test
        void shouldCreateTaskAndSubmitToWes() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_TASK_001"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU001", 10, "WH001"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_INT_001", items, 5);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            assertTrue(result.isSuccess());
            verify(wesPort).submitPickingTask(any(PickingTask.class));

            Optional<PickingTask> foundTask = pickingTaskRepository.findById(result.getTaskId());
            assertTrue(foundTask.isPresent());
            assertEquals(TaskStatus.SUBMITTED, foundTask.get().getStatus());
            assertEquals("ORDER_INT_001", foundTask.get().getOrderId());
        }

        @Test
        void shouldPersistTaskToDatabase() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_TASK_002"));

            List<TaskItemDto> items =
                    List.of(
                            new TaskItemDto("SKU100", 5, "A-01-01"),
                            new TaskItemDto("SKU101", 3, "A-01-02"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_INT_002", items, 7);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            Optional<PickingTask> persistedTask =
                    pickingTaskRepository.findById(result.getTaskId());
            assertTrue(persistedTask.isPresent());

            PickingTask task = persistedTask.get();
            assertEquals(2, task.getItems().size());
            assertEquals("SKU100", task.getItems().get(0).getSku());
            assertEquals(5, task.getItems().get(0).getQuantity());
            assertEquals("WES_TASK_002", task.getWesTaskId().getValue());
        }

        @Test
        void shouldCreateMultipleTasksForSameOrder() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_TASK_003"), WesTaskId.of("WES_TASK_004"));

            List<TaskItemDto> items1 = List.of(new TaskItemDto("SKU200", 8, "B-01-01"));
            CreatePickingTaskForOrderCommand command1 =
                    new CreatePickingTaskForOrderCommand("ORDER_INT_003", items1, 4);

            List<TaskItemDto> items2 = List.of(new TaskItemDto("SKU201", 12, "B-01-02"));
            CreatePickingTaskForOrderCommand command2 =
                    new CreatePickingTaskForOrderCommand("ORDER_INT_003", items2, 6);

            WesOperationResultDto result1 =
                    pickingTaskApplicationService.createPickingTaskForOrder(command1);
            WesOperationResultDto result2 =
                    pickingTaskApplicationService.createPickingTaskForOrder(command2);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            assertNotEquals(result1.getTaskId(), result2.getTaskId());

            List<PickingTask> tasks = pickingTaskRepository.findByOrderId("ORDER_INT_003");
            assertEquals(2, tasks.size());
        }

        @Test
        void shouldHandleWesSubmissionException() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenThrow(new WesSubmissionException("WES endpoint not available"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU300", 2, "C-01-01"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_INT_004", items, 3);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            assertFalse(result.isSuccess());
            assertNotNull(result.getErrorMessage());
        }
    }

    @Nested
    class fullTaskLifecycle {

        @Test
        void shouldCompleteFullTaskLifecycleFromCreationToCompletion() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_LIFECYCLE_001"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU400", 15, "D-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_LIFECYCLE_001", items, 8);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);
            String taskId = result.getTaskId();
            Optional<PickingTask> createdTask = pickingTaskRepository.findById(taskId);
            assertTrue(createdTask.isPresent());
            assertEquals(TaskStatus.SUBMITTED, createdTask.get().getStatus());

            UpdateTaskStatusFromWesCommand updateToInProgress =
                    new UpdateTaskStatusFromWesCommand(taskId, TaskStatus.IN_PROGRESS);
            pickingTaskApplicationService.updateTaskStatusFromWes(updateToInProgress);

            Optional<PickingTask> inProgressTask = pickingTaskRepository.findById(taskId);
            assertTrue(inProgressTask.isPresent());
            assertEquals(TaskStatus.IN_PROGRESS, inProgressTask.get().getStatus());

            MarkTaskCompletedCommand completeCommand = new MarkTaskCompletedCommand(taskId);
            pickingTaskApplicationService.markTaskCompleted(completeCommand);

            Optional<PickingTask> completedTask = pickingTaskRepository.findById(taskId);
            assertTrue(completedTask.isPresent());
            assertEquals(TaskStatus.COMPLETED, completedTask.get().getStatus());
            assertNotNull(completedTask.get().getCompletedAt());
        }

        @Test
        void shouldHandleTaskCancellation() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_CANCEL_001"));
            doNothing().when(wesPort).cancelTask(any());

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU500", 6, "E-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_CANCEL_001", items, 2);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);
            String taskId = result.getTaskId();

            CancelTaskCommand cancelCommand =
                    new CancelTaskCommand(taskId, "Customer requested cancellation");
            pickingTaskApplicationService.cancelTask(cancelCommand);

            Optional<PickingTask> canceledTask = pickingTaskRepository.findById(taskId);
            assertTrue(canceledTask.isPresent());
            assertEquals(TaskStatus.CANCELED, canceledTask.get().getStatus());
            assertEquals("Customer requested cancellation", canceledTask.get().getFailureReason());
            verify(wesPort).cancelTask(any());
        }

        @Test
        void shouldHandleTaskFailure() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_FAIL_001"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU600", 9, "F-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_FAIL_001", items, 10);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);
            String taskId = result.getTaskId();

            MarkTaskFailedCommand failCommand = new MarkTaskFailedCommand(taskId, "Item damaged");
            pickingTaskApplicationService.markTaskFailed(failCommand);

            Optional<PickingTask> failedTask = pickingTaskRepository.findById(taskId);
            assertTrue(failedTask.isPresent());
            assertEquals(TaskStatus.FAILED, failedTask.get().getStatus());
            assertEquals("Item damaged", failedTask.get().getFailureReason());
        }
    }

    @Nested
    class priorityManagement {

        @Test
        void shouldAdjustPriorityAndUpdateWes() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_PRIORITY_001"));
            doNothing().when(wesPort).updateTaskPriority(any(), anyInt());

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU700", 11, "G-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_PRIORITY_001", items, 3);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);
            String taskId = result.getTaskId();

            Optional<PickingTask> taskBeforeUpdate = pickingTaskRepository.findById(taskId);
            assertTrue(taskBeforeUpdate.isPresent());
            assertEquals(3, taskBeforeUpdate.get().getPriority());

            AdjustTaskPriorityCommand adjustCommand = new AdjustTaskPriorityCommand(taskId, 9);
            pickingTaskApplicationService.adjustTaskPriority(adjustCommand);

            Optional<PickingTask> taskAfterUpdate = pickingTaskRepository.findById(taskId);
            assertTrue(taskAfterUpdate.isPresent());
            assertEquals(9, taskAfterUpdate.get().getPriority());
            verify(wesPort).updateTaskPriority(any(), eq(9));
        }

        @Test
        void shouldHandlePriorityUpdateForMultipleTasks() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_PRIORITY_002"), WesTaskId.of("WES_PRIORITY_003"));
            doNothing().when(wesPort).updateTaskPriority(any(), anyInt());

            List<TaskItemDto> items1 = List.of(new TaskItemDto("SKU800", 4, "H-01-01"));
            CreatePickingTaskForOrderCommand command1 =
                    new CreatePickingTaskForOrderCommand("ORDER_PRIORITY_002", items1, 5);
            WesOperationResultDto result1 =
                    pickingTaskApplicationService.createPickingTaskForOrder(command1);
            String taskId1 = result1.getTaskId();

            List<TaskItemDto> items2 = List.of(new TaskItemDto("SKU801", 7, "H-01-02"));
            CreatePickingTaskForOrderCommand command2 =
                    new CreatePickingTaskForOrderCommand("ORDER_PRIORITY_002", items2, 5);
            WesOperationResultDto result2 =
                    pickingTaskApplicationService.createPickingTaskForOrder(command2);
            String taskId2 = result2.getTaskId();

            AdjustTaskPriorityCommand adjustCommand1 = new AdjustTaskPriorityCommand(taskId1, 10);
            pickingTaskApplicationService.adjustTaskPriority(adjustCommand1);

            AdjustTaskPriorityCommand adjustCommand2 = new AdjustTaskPriorityCommand(taskId2, 10);
            pickingTaskApplicationService.adjustTaskPriority(adjustCommand2);

            List<PickingTask> tasks = pickingTaskRepository.findByOrderId("ORDER_PRIORITY_002");
            assertEquals(2, tasks.size());
            assertTrue(tasks.stream().allMatch(t -> t.getPriority() == 10));
        }

        @Test
        void shouldHandleWesPriorityUpdateException() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_PRIORITY_FAIL"));
            doThrow(
                            new WesPriorityUpdateException(
                                    WesTaskId.of("WES_PRIORITY_FAIL"), 8, "WES server error"))
                    .when(wesPort)
                    .updateTaskPriority(any(), eq(8));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU900", 13, "I-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_PRIORITY_FAIL", items, 2);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);

            AdjustTaskPriorityCommand adjustCommand =
                    new AdjustTaskPriorityCommand(result.getTaskId(), 8);

            assertThrows(
                    WesPriorityUpdateException.class,
                    () -> pickingTaskApplicationService.adjustTaskPriority(adjustCommand));
        }
    }

    @Nested
    class exceptionHandling {

        @Test
        void shouldHandleWesTaskCancellationException() {
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_CANCEL_FAIL"));
            doThrow(
                            new WesTaskCancellationException(
                                    WesTaskId.of("WES_CANCEL_FAIL"), "WES server error"))
                    .when(wesPort)
                    .cancelTask(any());

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU970", 16, "L-01-01"));
            CreatePickingTaskForOrderCommand createCommand =
                    new CreatePickingTaskForOrderCommand("ORDER_CANCEL_FAIL", items, 7);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(createCommand);

            CancelTaskCommand cancelCommand =
                    new CancelTaskCommand(result.getTaskId(), "Cancel request");

            assertThrows(
                    WesTaskCancellationException.class,
                    () -> pickingTaskApplicationService.cancelTask(cancelCommand));
        }
    }
}
