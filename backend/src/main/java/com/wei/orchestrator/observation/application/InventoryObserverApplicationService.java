package com.wei.orchestrator.observation.application;

import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.observation.application.command.CreateInventoryObserverCommand;
import com.wei.orchestrator.observation.application.command.PollInventorySnapshotCommand;
import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationRule;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.repository.InventoryObserverRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryObserverApplicationService {

    private final InventoryObserverRepository inventoryObserverRepository;
    private final InventoryPort inventoryPort;
    private final ApplicationEventPublisher eventPublisher;

    public InventoryObserverApplicationService(
            InventoryObserverRepository inventoryObserverRepository,
            InventoryPort inventoryPort,
            ApplicationEventPublisher eventPublisher) {
        this.inventoryObserverRepository = inventoryObserverRepository;
        this.inventoryPort = inventoryPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String createInventoryObserver(CreateInventoryObserverCommand command) {
        ObservationRule observationRule =
                new ObservationRule(command.getThresholdPercent(), command.getCheckFrequency());

        PollingInterval pollingInterval = new PollingInterval(command.getPollingIntervalSeconds());

        InventoryObserver inventoryObserver =
                new InventoryObserver(command.getObserverId(), observationRule, pollingInterval);

        InventoryObserver savedObserver = inventoryObserverRepository.save(inventoryObserver);

        return savedObserver.getObserverId();
    }

    @Transactional
    public void pollInventorySnapshot(PollInventorySnapshotCommand command) {
        InventoryObserver inventoryObserver =
                inventoryObserverRepository
                        .findById(command.getObserverId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "InventoryObserver not found: "
                                                        + command.getObserverId()));

        if (!inventoryObserver.shouldPoll()) {
            return;
        }

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        inventoryObserverRepository.save(inventoryObserver);

        List<Object> domainEvents = inventoryObserver.getDomainEvents();
        domainEvents.forEach(eventPublisher::publishEvent);

        inventoryObserver.clearDomainEvents();
    }

    @Transactional
    public void pollAllActiveObservers() {
        List<InventoryObserver> activeObservers = inventoryObserverRepository.findAllActive();

        for (InventoryObserver observer : activeObservers) {
            PollInventorySnapshotCommand command =
                    new PollInventorySnapshotCommand(observer.getObserverId());
            pollInventorySnapshot(command);
        }
    }

    @Transactional
    public void activateObserver(String observerId) {
        InventoryObserver inventoryObserver =
                inventoryObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "InventoryObserver not found: " + observerId));

        inventoryObserver.activate();
        inventoryObserverRepository.save(inventoryObserver);
    }

    @Transactional
    public void deactivateObserver(String observerId) {
        InventoryObserver inventoryObserver =
                inventoryObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "InventoryObserver not found: " + observerId));

        inventoryObserver.deactivate();
        inventoryObserverRepository.save(inventoryObserver);
    }
}
