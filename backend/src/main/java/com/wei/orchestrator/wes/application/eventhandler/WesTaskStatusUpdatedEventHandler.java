package com.wei.orchestrator.wes.application.eventhandler;

import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.MarkTaskCanceledCommand;
import com.wei.orchestrator.wes.application.command.MarkTaskCompletedCommand;
import com.wei.orchestrator.wes.application.command.MarkTaskFailedCommand;
import com.wei.orchestrator.wes.application.command.UpdateTaskStatusFromWesCommand;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WesTaskStatusUpdatedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(WesTaskStatusUpdatedEventHandler.class);

    private final PickingTaskApplicationService pickingTaskApplicationService;
    private final PickingTaskRepository pickingTaskRepository;

    public WesTaskStatusUpdatedEventHandler(
            PickingTaskApplicationService pickingTaskApplicationService,
            PickingTaskRepository pickingTaskRepository) {
        this.pickingTaskApplicationService = pickingTaskApplicationService;
        this.pickingTaskRepository = pickingTaskRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleWesTaskStatusUpdated(WesTaskStatusUpdatedEvent event) {
        String taskId = event.getTaskId();
        TriggerContext triggerContext = event.getTriggerContext();

        logger.info(
                "Handling WesTaskStatusUpdatedEvent: taskId={}, newStatus={}",
                taskId,
                event.getNewStatus());

        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(taskId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "PickingTask not found for taskId: " + taskId));

        switch (event.getNewStatus()) {
            case COMPLETED -> {
                if (pickingTask.getStatus() != event.getNewStatus()) {
                    MarkTaskCompletedCommand command = new MarkTaskCompletedCommand(taskId);
                    pickingTaskApplicationService.markTaskCompleted(command, triggerContext);
                }
            }
            case FAILED -> {
                if (pickingTask.getStatus() != event.getNewStatus()) {
                    MarkTaskFailedCommand command =
                            new MarkTaskFailedCommand(taskId, "Failed in WES");
                    pickingTaskApplicationService.markTaskFailed(command, triggerContext);
                }
            }
            case CANCELED -> {
                if (pickingTask.getStatus() != event.getNewStatus()) {
                    MarkTaskCanceledCommand command =
                            new MarkTaskCanceledCommand(taskId, "Canceled in WES");
                    pickingTaskApplicationService.markTaskCanceled(command, triggerContext);
                }
            }
            default -> {
                UpdateTaskStatusFromWesCommand command =
                        new UpdateTaskStatusFromWesCommand(taskId, event.getNewStatus());
                pickingTaskApplicationService.updateTaskStatusFromWes(command);
            }
        }

        logger.info(
                "Successfully handled task status update: taskId={}, wesTaskId={}, newStatus={}",
                taskId,
                taskId,
                event.getNewStatus());
    }
}
