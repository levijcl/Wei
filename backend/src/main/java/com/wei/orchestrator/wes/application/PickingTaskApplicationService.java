package com.wei.orchestrator.wes.application;

import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.application.command.*;
import com.wei.orchestrator.wes.application.dto.WesOperationResultDto;
import com.wei.orchestrator.wes.domain.event.PickingTaskCreatedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskFailedEvent;
import com.wei.orchestrator.wes.domain.event.PickingTaskSubmittedEvent;
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
            CreatePickingTaskForOrderCommand command, TriggerContext triggerContext) {
        List<TaskItem> items =
                command.getItems().stream()
                        .map(dto -> TaskItem.of(dto.getSku(), dto.getQuantity(), dto.getLocation()))
                        .collect(Collectors.toList());

        TriggerContext context = triggerContext != null ? triggerContext : TriggerContext.manual();

        PickingTask pickingTask =
                PickingTask.createForOrder(command.getOrderId(), items, command.getPriority());

        PickingTask savedTask = pickingTaskRepository.save(pickingTask);

        try {
            WesTaskId wesTaskId = wesPort.submitPickingTask(savedTask);
            savedTask.submitToWes(wesTaskId);

            pickingTaskRepository.save(savedTask);
            publishEventsWithContext(savedTask, context);

            return WesOperationResultDto.success(savedTask.getTaskId());
        } catch (Exception e) {
            savedTask.markFailed(e.getMessage());
            pickingTaskRepository.save(savedTask);
            publishEventsWithContext(savedTask, context);

            return WesOperationResultDto.failure(e.getMessage());
        }
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
    public void markTaskCanceled(MarkTaskCanceledCommand command) {
        PickingTask pickingTask =
                pickingTaskRepository
                        .findById(command.getTaskId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Picking task not found: " + command.getTaskId()));

        pickingTask.markCanceled(command.getReason());

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

    private void publishEventsWithContext(PickingTask pickingTask, TriggerContext triggerContext) {
        TriggerContext context = triggerContext != null ? triggerContext : TriggerContext.manual();

        pickingTask.getDomainEvents().stream()
                .map(event -> enrichWithTriggerContext(event, context))
                .forEach(eventPublisher::publishEvent);
        pickingTask.clearDomainEvents();
    }

    private Object enrichWithTriggerContext(Object event, TriggerContext triggerContext) {
        TriggerContext newContext =
                TriggerContext.of(
                        "OrderReservedEvent",
                        triggerContext.getCorrelationId(),
                        triggerContext.getTriggerBy());

        if (event instanceof PickingTaskCreatedEvent original) {
            return new PickingTaskCreatedEvent(
                    original.getTaskId(),
                    original.getOrderId(),
                    original.getOrigin(),
                    original.getPriority(),
                    original.getItems(),
                    original.getOccurredAt(),
                    newContext);
        } else if (event instanceof PickingTaskSubmittedEvent original) {
            return new PickingTaskSubmittedEvent(
                    original.getTaskId(),
                    original.getWesTaskId(),
                    original.getOrigin(),
                    original.getOccurredAt(),
                    newContext);
        } else if (event instanceof PickingTaskFailedEvent original) {
            return new PickingTaskFailedEvent(
                    original.getTaskId(),
                    original.getWesTaskId(),
                    original.getOrderId(),
                    original.getOrigin(),
                    original.getReason(),
                    original.getOccurredAt(),
                    newContext);
        }
        return event;
    }
}
