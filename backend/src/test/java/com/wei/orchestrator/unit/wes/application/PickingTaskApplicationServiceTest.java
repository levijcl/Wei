package com.wei.orchestrator.unit.wes.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.*;
import com.wei.orchestrator.wes.application.command.dto.TaskItemDto;
import com.wei.orchestrator.wes.application.dto.WesOperationResultDto;
import com.wei.orchestrator.wes.domain.exception.WesPriorityUpdateException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PickingTaskApplicationServiceTest {
    @Mock private PickingTaskRepository pickingTaskRepository;
    @Mock private WesPort wesPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private PickingTaskApplicationService pickingTaskApplicationService;

    @Nested
    class createPickingTaskForOrderTest {
        @Test
        void shouldCreatePickingTaskForOrderSuccessfully() {
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_TASK_001"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU001", 10, "WH001"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_001", items, 1);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            assertNotNull(result.getTaskId());

            verify(pickingTaskRepository, times(2)).save(any(PickingTask.class));
            verify(wesPort).submitPickingTask(any(PickingTask.class));
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }

        @Test
        void shouldCreatePickingTaskForOrderFailed() {
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenThrow(new RuntimeException("Connection fail"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU001", 10, "WH001"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_001", items, 1);

            WesOperationResultDto result =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            assertFalse(result.isSuccess());
            assertEquals("Connection fail", result.getErrorMessage());
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }
    }

    @Nested
    class updateTaskStatusFromWesTest {
        @Test
        void shouldUpdateTaskStatusSuccessfully() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            UpdateTaskStatusFromWesCommand command =
                    new UpdateTaskStatusFromWesCommand("TASK_001", TaskStatus.COMPLETED);

            pickingTaskApplicationService.updateTaskStatusFromWes(command);

            verify(task).updateStatusFromWes(TaskStatus.COMPLETED);
            verify(pickingTaskRepository).save(task);
            verify(task).getDomainEvents();
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            UpdateTaskStatusFromWesCommand command =
                    new UpdateTaskStatusFromWesCommand("NON_EXISTENT", TaskStatus.COMPLETED);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskApplicationService.updateTaskStatusFromWes(command));

            assertTrue(exception.getMessage().contains("Picking task not found"));
            verify(pickingTaskRepository, never()).save(any());
        }

        @Test
        void shouldPublishEventsAfterStatusUpdate() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(task.getDomainEvents()).thenReturn(List.of(new Object()));

            UpdateTaskStatusFromWesCommand command =
                    new UpdateTaskStatusFromWesCommand("TASK_001", TaskStatus.IN_PROGRESS);

            pickingTaskApplicationService.updateTaskStatusFromWes(command);

            verify(eventPublisher).publishEvent(any(Object.class));
            verify(task).clearDomainEvents();
        }
    }

    @Nested
    class adjustTaskPriorityTest {
        @Test
        void shouldAdjustPriorityAndUpdateWesSuccessfully() {
            PickingTask task = mock(PickingTask.class);
            when(task.getWesTaskId()).thenReturn(WesTaskId.of("WES_TASK_001"));
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdjustTaskPriorityCommand command = new AdjustTaskPriorityCommand("TASK_001", 9);

            pickingTaskApplicationService.adjustTaskPriority(command);

            verify(task).adjustPriority(9);
            verify(wesPort).updateTaskPriority(WesTaskId.of("WES_TASK_001"), 9);
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldAdjustPriorityWithoutWesTaskId() {
            PickingTask task = mock(PickingTask.class);
            when(task.getWesTaskId()).thenReturn(null);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AdjustTaskPriorityCommand command = new AdjustTaskPriorityCommand("TASK_001", 5);

            pickingTaskApplicationService.adjustTaskPriority(command);

            verify(task).adjustPriority(5);
            verify(wesPort, never()).updateTaskPriority(any(), anyInt());
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            AdjustTaskPriorityCommand command = new AdjustTaskPriorityCommand("NON_EXISTENT", 7);

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskApplicationService.adjustTaskPriority(command));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldPropagateWesPriorityUpdateException() {
            PickingTask task = mock(PickingTask.class);
            WesTaskId wesTaskId = WesTaskId.of("WES_TASK_001");
            when(task.getWesTaskId()).thenReturn(wesTaskId);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            doThrow(new WesPriorityUpdateException(wesTaskId, 8, "WES server error"))
                    .when(wesPort)
                    .updateTaskPriority(wesTaskId, 8);

            AdjustTaskPriorityCommand command = new AdjustTaskPriorityCommand("TASK_001", 8);

            assertThrows(
                    WesPriorityUpdateException.class,
                    () -> pickingTaskApplicationService.adjustTaskPriority(command));

            verify(task).adjustPriority(8);
            verify(pickingTaskRepository, never()).save(any());
        }
    }

    @Nested
    class markTaskCompletedTest {
        @Test
        void shouldMarkTaskCompletedSuccessfully() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MarkTaskCompletedCommand command = new MarkTaskCompletedCommand("TASK_001");

            pickingTaskApplicationService.markTaskCompleted(command);

            verify(task).markCompleted();
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            MarkTaskCompletedCommand command = new MarkTaskCompletedCommand("NON_EXISTENT");

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskApplicationService.markTaskCompleted(command));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldPublishCompletedEvent() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(task.getDomainEvents()).thenReturn(List.of(new Object()));

            MarkTaskCompletedCommand command = new MarkTaskCompletedCommand("TASK_001");

            pickingTaskApplicationService.markTaskCompleted(command);

            verify(eventPublisher).publishEvent(any(Object.class));
            verify(task).clearDomainEvents();
        }
    }

    @Nested
    class markTaskFailedTest {
        @Test
        void shouldMarkTaskFailedWithReason() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MarkTaskFailedCommand command =
                    new MarkTaskFailedCommand("TASK_001", "Item out of stock");

            pickingTaskApplicationService.markTaskFailed(command);

            verify(task).markFailed("Item out of stock");
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            MarkTaskFailedCommand command =
                    new MarkTaskFailedCommand("NON_EXISTENT", "Some reason");

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskApplicationService.markTaskFailed(command));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldPublishFailedEvent() {
            PickingTask task = mock(PickingTask.class);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(task.getDomainEvents()).thenReturn(List.of(new Object()));

            MarkTaskFailedCommand command =
                    new MarkTaskFailedCommand("TASK_001", "Hardware failure");

            pickingTaskApplicationService.markTaskFailed(command);

            verify(eventPublisher).publishEvent(any(Object.class));
            verify(task).clearDomainEvents();
        }
    }

    @Nested
    class cancelTaskTest {
        @Test
        void shouldCancelTaskAndNotifyWesSuccessfully() {
            PickingTask task = mock(PickingTask.class);
            WesTaskId wesTaskId = WesTaskId.of("WES_TASK_001");
            when(task.getWesTaskId()).thenReturn(wesTaskId);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CancelTaskCommand command =
                    new CancelTaskCommand("TASK_001", "Customer requested cancellation");

            pickingTaskApplicationService.cancelTask(command);

            verify(task).cancel("Customer requested cancellation");
            verify(wesPort).cancelTask(wesTaskId);
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldCancelTaskWithoutWesTaskId() {
            PickingTask task = mock(PickingTask.class);
            when(task.getWesTaskId()).thenReturn(null);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            CancelTaskCommand command = new CancelTaskCommand("TASK_001", "Not yet submitted");

            pickingTaskApplicationService.cancelTask(command);

            verify(task).cancel("Not yet submitted");
            verify(wesPort, never()).cancelTask(any());
            verify(pickingTaskRepository).save(task);
        }

        @Test
        void shouldThrowExceptionWhenTaskNotFound() {
            when(pickingTaskRepository.findById("NON_EXISTENT")).thenReturn(Optional.empty());

            CancelTaskCommand command = new CancelTaskCommand("NON_EXISTENT", "Some reason");

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> pickingTaskApplicationService.cancelTask(command));

            assertTrue(exception.getMessage().contains("Picking task not found"));
        }

        @Test
        void shouldPropagateWesTaskCancellationException() {
            PickingTask task = mock(PickingTask.class);
            WesTaskId wesTaskId = WesTaskId.of("WES_TASK_001");
            when(task.getWesTaskId()).thenReturn(wesTaskId);
            when(pickingTaskRepository.findById("TASK_001")).thenReturn(Optional.of(task));
            doThrow(new WesTaskCancellationException(wesTaskId, "WES server error"))
                    .when(wesPort)
                    .cancelTask(wesTaskId);

            CancelTaskCommand command = new CancelTaskCommand("TASK_001", "Cancel");

            assertThrows(
                    WesTaskCancellationException.class,
                    () -> pickingTaskApplicationService.cancelTask(command));

            verify(task).cancel("Cancel");
            verify(pickingTaskRepository, never()).save(any());
        }
    }
}
