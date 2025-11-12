package com.wei.orchestrator.order.application;

import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.domain.service.OrderDomainService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderApplicationService {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderApplicationService(
            OrderRepository orderRepository,
            OrderDomainService orderDomainService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderDomainService = orderDomainService;
        this.eventPublisher = eventPublisher;
    }

    public Order createOrder(CreateOrderCommand command) {
        orderDomainService.validateOrderCreation(command.getOrderId());

        List<OrderLineItem> lineItems =
                command.getItems().stream()
                        .map(
                                dto ->
                                        new OrderLineItem(
                                                dto.getSku(), dto.getQuantity(), dto.getPrice()))
                        .collect(Collectors.toList());

        Order order = new Order(command.getOrderId(), lineItems);
        order.createOrder();

        if (command.getScheduledPickupTime() != null) {
            ScheduledPickupTime scheduledPickupTime =
                    new ScheduledPickupTime(command.getScheduledPickupTime());
            FulfillmentLeadTime fulfillmentLeadTime =
                    command.getFulfillmentLeadTime() != null
                            ? new FulfillmentLeadTime(command.getFulfillmentLeadTime())
                            : FulfillmentLeadTime.defaultLeadTime();

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, LocalDateTime.now());
        }

        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();

        return order;
    }

    public void initiateFulfillment(InitiateFulfillmentCommand command) {
        Order order =
                orderRepository
                        .findById(command.getOrderId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Order not found: " + command.getOrderId()));

        order.markReadyForFulfillment();
        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();
    }

    public void markOrderItemsAsPickingCompleted(
            String orderId, List<String> skus, String wesTaskId) {
        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Order not found: " + orderId));

        order.markItemsAsPickingCompleted(skus, wesTaskId);
        orderRepository.save(order);

        order.getDomainEvents().forEach(eventPublisher::publishEvent);
        order.clearDomainEvents();
    }
}
