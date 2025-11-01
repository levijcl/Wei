package com.wei.orchestrator.observation.application;

import com.wei.orchestrator.observation.application.command.CreateOrderObserverCommand;
import com.wei.orchestrator.observation.application.command.PollOrderSourceCommand;
import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import com.wei.orchestrator.observation.domain.repository.OrderObserverRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderObserverApplicationService {

    private final OrderObserverRepository orderObserverRepository;
    private final OrderSourcePort orderSourcePort;
    private final ApplicationEventPublisher eventPublisher;

    public OrderObserverApplicationService(
            OrderObserverRepository orderObserverRepository,
            OrderSourcePort orderSourcePort,
            ApplicationEventPublisher eventPublisher) {
        this.orderObserverRepository = orderObserverRepository;
        this.orderSourcePort = orderSourcePort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String createOrderObserver(CreateOrderObserverCommand command) {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint(
                        command.getJdbcUrl(), command.getUsername(), command.getPassword());

        PollingInterval pollingInterval = new PollingInterval(command.getPollingIntervalSeconds());

        OrderObserver orderObserver =
                new OrderObserver(command.getObserverId(), sourceEndpoint, pollingInterval);

        OrderObserver savedObserver = orderObserverRepository.save(orderObserver);

        return savedObserver.getObserverId();
    }

    @Transactional
    public void pollOrderSource(PollOrderSourceCommand command) {
        OrderObserver orderObserver =
                orderObserverRepository
                        .findById(command.getObserverId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "OrderObserver not found: "
                                                        + command.getObserverId()));

        orderObserver.pollOrderSource(orderSourcePort);

        orderObserverRepository.save(orderObserver);

        List<Object> domainEvents = orderObserver.getDomainEvents();
        domainEvents.forEach(eventPublisher::publishEvent);

        orderObserver.clearDomainEvents();
    }

    @Transactional
    public void pollAllActiveObservers() {
        List<OrderObserver> activeObservers = orderObserverRepository.findAllActive();

        for (OrderObserver observer : activeObservers) {
            PollOrderSourceCommand command = new PollOrderSourceCommand(observer.getObserverId());
            pollOrderSource(command);
        }
    }

    @Transactional
    public void activateObserver(String observerId) {
        OrderObserver orderObserver =
                orderObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "OrderObserver not found: " + observerId));

        orderObserver.activate();
        orderObserverRepository.save(orderObserver);
    }

    @Transactional
    public void deactivateObserver(String observerId) {
        OrderObserver orderObserver =
                orderObserverRepository
                        .findById(observerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "OrderObserver not found: " + observerId));

        orderObserver.deactivate();
        orderObserverRepository.save(orderObserver);
    }
}
