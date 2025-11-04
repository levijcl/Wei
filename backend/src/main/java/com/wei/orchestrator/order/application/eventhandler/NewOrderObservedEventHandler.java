package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.CreateOrderCommand.OrderLineItemDto;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NewOrderObservedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(NewOrderObservedEventHandler.class);

    private final OrderApplicationService orderApplicationService;
    private final OrderRepository orderRepository;

    public NewOrderObservedEventHandler(
            OrderApplicationService orderApplicationService, OrderRepository orderRepository) {
        this.orderApplicationService = orderApplicationService;
        this.orderRepository = orderRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleNewOrderObserved(NewOrderObservedEvent event) {
        ObservationResult observedOrder = event.getObservedOrder();
        String orderId = observedOrder.getOrderId();

        logger.info(
                "Handling NewOrderObservedEvent for order: {} from observer: {}",
                orderId,
                event.getObserverId());

        if (orderRepository.findById(orderId).isPresent()) {
            logger.info("Order {} already exists, skipping creation (idempotency check)", orderId);
            return;
        }

        CreateOrderCommand command = mapToCreateOrderCommand(observedOrder);

        orderApplicationService.createOrder(command);

        logger.info(
                "Successfully created order {} from observed event at {}",
                orderId,
                event.getOccurredAt());
    }

    private CreateOrderCommand mapToCreateOrderCommand(ObservationResult observedOrder) {
        List<OrderLineItemDto> items =
                observedOrder.getItems().stream()
                        .map(this::mapToOrderLineItemDto)
                        .collect(Collectors.toList());

        return new CreateOrderCommand(observedOrder.getOrderId(), items);
    }

    private OrderLineItemDto mapToOrderLineItemDto(ObservedOrderItem observedItem) {
        return new OrderLineItemDto(
                observedItem.getSku(), observedItem.getQuantity(), observedItem.getPrice());
    }
}
