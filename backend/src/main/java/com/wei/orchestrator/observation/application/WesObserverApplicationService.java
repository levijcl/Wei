package com.wei.orchestrator.observation.application;

import com.wei.orchestrator.observation.application.command.CreateWesObserverCommand;
import com.wei.orchestrator.observation.application.command.PollWesTaskStatusCommand;
import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.observation.domain.model.WesObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.TaskEndpoint;
import com.wei.orchestrator.observation.domain.repository.WesObserverRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WesObserverApplicationService {

    private final WesObserverRepository wesObserverRepository;
    private final WesPort wesPort;
    private final PickingTaskRepository pickingTaskRepository;
    private final ApplicationEventPublisher eventPublisher;

    public WesObserverApplicationService(
            WesObserverRepository wesObserverRepository,
            WesPort wesPort,
            PickingTaskRepository pickingTaskRepository,
            ApplicationEventPublisher eventPublisher) {
        this.wesObserverRepository = wesObserverRepository;
        this.wesPort = wesPort;
        this.pickingTaskRepository = pickingTaskRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String createWesObserver(CreateWesObserverCommand command) {
        TaskEndpoint taskEndpoint =
                new TaskEndpoint(command.getTaskEndpointUrl(), command.getAuthToken());

        PollingInterval pollingInterval = new PollingInterval(command.getPollingIntervalSeconds());

        WesObserver wesObserver =
                new WesObserver(command.getObserverId(), taskEndpoint, pollingInterval);

        WesObserver savedObserver = wesObserverRepository.save(wesObserver);

        return savedObserver.getObserverId();
    }

    @Transactional
    public void pollWesTaskStatus(PollWesTaskStatusCommand command, TriggerContext triggerContext) {
        WesObserver wesObserver =
                wesObserverRepository
                        .findById(command.getObserverId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "WesObserver not found: "
                                                        + command.getObserverId()));

        if (!wesObserver.shouldPoll()) {
            return;
        }

        List<PickingTask> allPickingTasks = pickingTaskRepository.findAll();

        wesObserver.pollWesTaskStatus(wesPort, allPickingTasks);

        wesObserverRepository.save(wesObserver);

        TriggerContext context = triggerContext != null ? triggerContext : TriggerContext.manual();
        List<Object> domainEvents = wesObserver.getDomainEvents();
        domainEvents.stream()
                .map(event -> enrichWithTriggerContext(event, context))
                .forEach(eventPublisher::publishEvent);

        wesObserver.clearDomainEvents();
    }

    @Transactional
    public void pollAllActiveObservers() {
        List<WesObserver> activeObservers = wesObserverRepository.findAllActive();

        TriggerContext scheduledContext = TriggerContext.scheduled("WesObserver");

        for (WesObserver observer : activeObservers) {
            PollWesTaskStatusCommand command =
                    new PollWesTaskStatusCommand(observer.getObserverId());
            pollWesTaskStatus(command, scheduledContext);
        }
    }

    private Object enrichWithTriggerContext(Object event, TriggerContext triggerContext) {
        if (event instanceof WesTaskStatusUpdatedEvent original) {
            return new WesTaskStatusUpdatedEvent(
                    original.getTaskId(), original.getNewStatus(), triggerContext);
        }
        return event;
    }

    @Transactional
    public void activateObserver(String observerId) {
        WesObserver wesObserver =
                wesObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "WesObserver not found: " + observerId));

        wesObserver.activate();
        wesObserverRepository.save(wesObserver);
    }

    @Transactional
    public void deactivateObserver(String observerId) {
        WesObserver wesObserver =
                wesObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "WesObserver not found: " + observerId));

        wesObserver.deactivate();
        wesObserverRepository.save(wesObserver);
    }
}
