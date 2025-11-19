package com.wei.orchestrator.unit.wes.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.MarkTaskCanceledCommand;
import com.wei.orchestrator.wes.application.command.MarkTaskCompletedCommand;
import com.wei.orchestrator.wes.application.command.MarkTaskFailedCommand;
import com.wei.orchestrator.wes.application.command.UpdateTaskStatusFromWesCommand;
import com.wei.orchestrator.wes.application.eventhandler.WesTaskStatusUpdatedEventHandler;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WesTaskStatusUpdatedEventHandlerTest {

    @Mock private PickingTaskApplicationService pickingTaskApplicationService;

    @Mock private PickingTaskRepository pickingTaskRepository;

    @InjectMocks private WesTaskStatusUpdatedEventHandler eventHandler;

    @Nested
    class handleWesTaskStatusUpdatedTest {

        @Test
        void shouldCallMarkTaskCompletedWhenStatusIsCompleted() {
            String wesTaskId = "WES-TASK-001";
            String taskId = "PICK-TASK-001";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.COMPLETED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-001");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskCompletedCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskCompletedCommand.class);
            verify(pickingTaskApplicationService).markTaskCompleted(captor.capture(), any());
            assertEquals(taskId, captor.getValue().getTaskId());

            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).cancelTask(any());
            verify(pickingTaskApplicationService, never()).updateTaskStatusFromWes(any());
        }

        @Test
        void shouldCallMarkTaskFailedWhenStatusIsFailed() {
            String wesTaskId = "WES-TASK-002";
            String taskId = "PICK-TASK-002";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.FAILED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-002");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskFailedCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskFailedCommand.class);
            verify(pickingTaskApplicationService).markTaskFailed(captor.capture(), any());
            assertEquals(taskId, captor.getValue().getTaskId());
            assertEquals("Failed in WES", captor.getValue().getReason());

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).cancelTask(any());
            verify(pickingTaskApplicationService, never()).updateTaskStatusFromWes(any());
        }

        @Test
        void shouldCallMarkTaskCanceledWhenStatusIsCanceled() {
            String wesTaskId = "WES-TASK-003";
            String taskId = "PICK-TASK-003";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.CANCELED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-003");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskCanceledCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskCanceledCommand.class);
            verify(pickingTaskApplicationService).markTaskCanceled(captor.capture(), any());
            assertEquals(taskId, captor.getValue().getTaskId());
            assertEquals("Canceled in WES", captor.getValue().getReason());

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).updateTaskStatusFromWes(any());
        }

        @Test
        void shouldCallUpdateTaskStatusFromWesWhenStatusIsInProgress() {
            String wesTaskId = "WES-TASK-004";
            String taskId = "PICK-TASK-004";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.IN_PROGRESS);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-004");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<UpdateTaskStatusFromWesCommand> captor =
                    ArgumentCaptor.forClass(UpdateTaskStatusFromWesCommand.class);
            verify(pickingTaskApplicationService).updateTaskStatusFromWes(captor.capture());
            assertEquals(taskId, captor.getValue().getTaskId());
            assertEquals(TaskStatus.IN_PROGRESS, captor.getValue().getStatus());

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).cancelTask(any());
        }

        @Test
        void shouldCallUpdateTaskStatusFromWesWhenStatusIsSubmitted() {
            String wesTaskId = "WES-TASK-005";
            String taskId = "PICK-TASK-005";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.SUBMITTED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-005");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<UpdateTaskStatusFromWesCommand> captor =
                    ArgumentCaptor.forClass(UpdateTaskStatusFromWesCommand.class);
            verify(pickingTaskApplicationService).updateTaskStatusFromWes(captor.capture());
            assertEquals(taskId, captor.getValue().getTaskId());
            assertEquals(TaskStatus.SUBMITTED, captor.getValue().getStatus());

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).cancelTask(any());
        }

        @Test
        void shouldCallUpdateTaskStatusFromWesWhenStatusIsPending() {
            String wesTaskId = "WES-TASK-006";
            String taskId = "PICK-TASK-006";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.PENDING);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-006");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<UpdateTaskStatusFromWesCommand> captor =
                    ArgumentCaptor.forClass(UpdateTaskStatusFromWesCommand.class);
            verify(pickingTaskApplicationService).updateTaskStatusFromWes(captor.capture());
            assertEquals(taskId, captor.getValue().getTaskId());
            assertEquals(TaskStatus.PENDING, captor.getValue().getStatus());

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).cancelTask(any());
        }

        @Test
        void shouldThrowExceptionWhenPickingTaskNotFoundForWesTaskId() {
            String wesTaskId = "WES-TASK-999";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.COMPLETED);

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of());

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleWesTaskStatusUpdated(event);
                            });

            assertTrue(exception.getMessage().contains("PickingTask not found for wesTaskId"));
            assertTrue(exception.getMessage().contains(wesTaskId));

            verify(pickingTaskApplicationService, never()).markTaskCompleted(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskFailed(any(), any());
            verify(pickingTaskApplicationService, never()).markTaskCanceled(any(), any());
            verify(pickingTaskApplicationService, never()).updateTaskStatusFromWes(any());
        }

        @Test
        void shouldUseFirstPickingTaskWhenMultipleTasksFoundForWesTaskId() {
            String wesTaskId = "WES-TASK-007";
            String taskId1 = "PICK-TASK-007-A";
            String taskId2 = "PICK-TASK-007-B";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.COMPLETED);

            PickingTask pickingTask1 = createPickingTask(taskId1, "ORDER-007");
            pickingTask1.submitToWes(WesTaskId.of(wesTaskId));

            PickingTask pickingTask2 = createPickingTask(taskId2, "ORDER-007");
            pickingTask2.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId))
                    .thenReturn(List.of(pickingTask1, pickingTask2));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskCompletedCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskCompletedCommand.class);
            verify(pickingTaskApplicationService).markTaskCompleted(captor.capture(), any());
            assertEquals(taskId1, captor.getValue().getTaskId());
        }

        @Test
        void shouldHandleCompletedStatusCorrectly() {
            String wesTaskId = "WES-TASK-008";
            String taskId = "PICK-TASK-008";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.COMPLETED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-008");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            verify(pickingTaskRepository).findByWesTaskId(wesTaskId);
            verify(pickingTaskApplicationService)
                    .markTaskCompleted(any(MarkTaskCompletedCommand.class), any());
        }

        @Test
        void shouldHandleFailedStatusWithCorrectReason() {
            String wesTaskId = "WES-TASK-009";
            String taskId = "PICK-TASK-009";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.FAILED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-009");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskFailedCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskFailedCommand.class);
            verify(pickingTaskApplicationService).markTaskFailed(captor.capture(), any());
            assertEquals("Failed in WES", captor.getValue().getReason());
        }

        @Test
        void shouldHandleCanceledStatusWithCorrectReason() {
            String wesTaskId = "WES-TASK-010";
            String taskId = "PICK-TASK-010";

            WesTaskStatusUpdatedEvent event =
                    new WesTaskStatusUpdatedEvent(wesTaskId, TaskStatus.CANCELED);

            PickingTask pickingTask = createPickingTask(taskId, "ORDER-010");
            pickingTask.submitToWes(WesTaskId.of(wesTaskId));

            when(pickingTaskRepository.findByWesTaskId(wesTaskId)).thenReturn(List.of(pickingTask));

            eventHandler.handleWesTaskStatusUpdated(event);

            ArgumentCaptor<MarkTaskCanceledCommand> captor =
                    ArgumentCaptor.forClass(MarkTaskCanceledCommand.class);
            verify(pickingTaskApplicationService).markTaskCanceled(captor.capture(), any());
            assertEquals("Canceled in WES", captor.getValue().getReason());
        }
    }

    private PickingTask createPickingTask(String taskId, String orderId) {
        List<TaskItem> items = List.of(TaskItem.of("SKU-001", 10, "WH-001"));
        PickingTask task = PickingTask.createForOrder(orderId, items, 5);
        task.setTaskId(taskId);
        return task;
    }
}
