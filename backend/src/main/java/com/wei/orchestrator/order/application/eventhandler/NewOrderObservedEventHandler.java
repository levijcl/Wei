package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.translator.ObservationToOrderTranslator;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NewOrderObservedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(NewOrderObservedEventHandler.class);

    private final OrderApplicationService orderApplicationService;
    private final OrderRepository orderRepository;
    private final ObservationToOrderTranslator translator;

    public NewOrderObservedEventHandler(
            OrderApplicationService orderApplicationService,
            OrderRepository orderRepository,
            ObservationToOrderTranslator translator) {
        this.orderApplicationService = orderApplicationService;
        this.orderRepository = orderRepository;
        this.translator = translator;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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

        CreateOrderCommand command = translator.translate(observedOrder);

        orderApplicationService.createOrder(command, event.getTriggerContext());

        logger.info(
                "Successfully created order {} from observed event at {}",
                orderId,
                event.getOccurredAt());
    }
}
