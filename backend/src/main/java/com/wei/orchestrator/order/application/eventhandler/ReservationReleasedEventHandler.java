package com.wei.orchestrator.order.application.eventhandler;

import com.wei.orchestrator.inventory.domain.event.ReservationReleasedEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ReservationReleasedEventHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ReservationReleasedEventHandler.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationReleasedEventHandler(
            OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReservationReleased(ReservationReleasedEvent event) {
        String orderId = event.getOrderId();

        logger.info(
                "Handling ReservationReleasedEvent for order: {}, transactionId: {}",
                orderId,
                event.getTransactionId());

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Order not found for reservation release: "
                                                        + orderId));

        order.resetLineItemReservations();

        orderRepository.save(order);

        eventPublisher.publishEvent(order.getDomainEvents());
        order.clearDomainEvents();

        logger.info("Reset reservation status for order {} after inventory release", orderId);
    }
}
