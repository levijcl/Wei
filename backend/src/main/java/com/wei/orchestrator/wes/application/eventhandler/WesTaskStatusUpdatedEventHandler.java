package com.wei.orchestrator.wes.application.eventhandler;

import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
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
        String wesTaskId = event.getTaskId();

        logger.info(
                "Handling WesTaskStatusUpdatedEvent: wesTaskId={}, newStatus={}",
                wesTaskId,
                event.getNewStatus());

        String taskId =
                pickingTaskRepository.findByWesTaskId(wesTaskId).stream()
                        .findFirst()
                        .map(PickingTask::getTaskId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "PickingTask not found for wesTaskId: "
                                                        + wesTaskId));

        UpdateTaskStatusFromWesCommand command =
                new UpdateTaskStatusFromWesCommand(taskId, event.getNewStatus());

        pickingTaskApplicationService.updateTaskStatusFromWes(command);

        logger.info(
                "Successfully updated task status: taskId={}, wesTaskId={}, newStatus={}",
                taskId,
                wesTaskId,
                event.getNewStatus());
    }
}
