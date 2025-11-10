package com.wei.orchestrator.wes.application;

import com.wei.orchestrator.wes.application.command.*;
import com.wei.orchestrator.wes.application.dto.WesOperationResultDto;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskItem;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PickingTaskApplicationService {

    private final PickingTaskRepository pickingTaskRepository;
    private final WesPort wesPort;
    private final ApplicationEventPublisher eventPublisher;

    public PickingTaskApplicationService(
            PickingTaskRepository pickingTaskRepository,
            WesPort wesPort,
            ApplicationEventPublisher eventPublisher) {
        this.pickingTaskRepository = pickingTaskRepository;
        this.wesPort = wesPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public WesOperationResultDto createPickingTaskForOrder(
            CreatePickingTaskForOrderCommand command) {
        List<TaskItem> items =
                command.getItems().stream()
                        .map(dto -> TaskItem.of(dto.getSku(), dto.getQuantity(), dto.getLocation()))
                        .collect(Collectors.toList());

        PickingTask pickingTask =
                PickingTask.createForOrder(command.getOrderId(), items, command.getPriority());

        PickingTask savedTask = pickingTaskRepository.save(pickingTask);

        WesTaskId wesTaskId = wesPort.submitPickingTask(savedTask);

        savedTask.submitToWes(wesTaskId);

        pickingTaskRepository.save(savedTask);

        publishEvents(savedTask);

        return WesOperationResultDto.success(savedTask.getTaskId());
    }

    @Transactional
    public void updateTaskStatusFromWes(UpdateTaskStatusFromWesCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.updateStatusFromWes(command.getStatus());

        pickingTaskRepository.save(pickingTask);

        publishEvents(pickingTask);
    }

    @Transactional
    public void adjustTaskPriority(AdjustTaskPriorityCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.adjustPriority(command.getNewPriority());

        if (pickingTask.getWesTaskId() != null) {
            wesPort.updateTaskPriority(pickingTask.getWesTaskId(), command.getNewPriority());
        }

        pickingTaskRepository.save(pickingTask);

        publishEvents(pickingTask);
    }

    @Transactional
    public void markTaskCompleted(MarkTaskCompletedCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.markCompleted();

        pickingTaskRepository.save(pickingTask);

        publishEvents(pickingTask);
    }

    @Transactional
    public void markTaskFailed(MarkTaskFailedCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.markFailed(command.getReason());

        pickingTaskRepository.save(pickingTask);

        publishEvents(pickingTask);
    }

    @Transactional
    public void cancelTask(CancelTaskCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.cancel(command.getReason());

        if (pickingTask.getWesTaskId() != null) {
            wesPort.cancelTask(pickingTask.getWesTaskId());
        }

        pickingTaskRepository.save(pickingTask);

        publishEvents(pickingTask);
    }

    private void publishEvents(PickingTask pickingTask) {
        pickingTask.getDomainEvents().forEach(eventPublisher::publishEvent);
        pickingTask.clearDomainEvents();
    }
}
