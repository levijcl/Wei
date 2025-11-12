package com.wei.orchestrator.observation.application;

import com.wei.orchestrator.observation.application.command.CreateWesObserverCommand;
import com.wei.orchestrator.observation.application.command.PollWesTaskStatusCommand;
import com.wei.orchestrator.observation.domain.model.WesObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.TaskEndpoint;
import com.wei.orchestrator.observation.domain.repository.WesObserverRepository;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import java.util.Map;
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
    public void pollWesTaskStatus(PollWesTaskStatusCommand command) {
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

        List<String> existingWesTaskIds = pickingTaskRepository.findAllWesTaskIds();
        Map<String, TaskStatus> existingTaskStatuses =
                pickingTaskRepository.findAllTaskStatusesByWesTaskId();

        wesObserver.pollWesTaskStatus(wesPort, existingWesTaskIds, existingTaskStatuses);

        wesObserverRepository.save(wesObserver);

        List<Object> domainEvents = wesObserver.getDomainEvents();
        domainEvents.forEach(eventPublisher::publishEvent);

        wesObserver.clearDomainEvents();
    }

    @Transactional
    public void pollAllActiveObservers() {
        List<WesObserver> activeObservers = wesObserverRepository.findAllActive();

        for (WesObserver observer : activeObservers) {
            PollWesTaskStatusCommand command =
                    new PollWesTaskStatusCommand(observer.getObserverId());
            pollWesTaskStatus(command);
        }
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
