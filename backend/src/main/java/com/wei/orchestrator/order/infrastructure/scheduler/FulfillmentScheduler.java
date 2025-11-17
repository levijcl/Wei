package com.wei.orchestrator.order.infrastructure.scheduler;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentScheduler {
    private static final Logger logger = LoggerFactory.getLogger(FulfillmentScheduler.class);
    private static final String LOCK_KEY = "order-fulfillment-initiation";

    private final OrderApplicationService orderApplicationService;
    private final OrderRepository orderRepository;
    private final LockRegistry lockRegistry;

    public FulfillmentScheduler(
            OrderApplicationService orderApplicationService,
            OrderRepository orderRepository,
            LockRegistry lockRegistry) {
        this.orderApplicationService = orderApplicationService;
        this.orderRepository = orderRepository;
        this.lockRegistry = lockRegistry;
    }

    @Scheduled(fixedDelayString = "${scheduler.order.fulfillment-delay:60000}")
    public void initiateFulfillment() {
        logger.info("Starting scheduled fulfillment initiation check");

        Lock lock = lockRegistry.obtain(LOCK_KEY);
        boolean lockAcquired = false;

        try {
            lockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

            if (!lockAcquired) {
                logger.warn(
                        "Could not acquire lock for fulfillment initiation, skipping this cycle");
                return;
            }

            processInitiation();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while acquiring lock", e);
        } catch (Exception e) {
            logger.error("Unexpected error during fulfillment initiation", e);
        } finally {
            if (lockAcquired) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    logger.error("Error releasing lock for: {}", LOCK_KEY, e);
                }
            }
        }
    }

    private void processInitiation() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> readyOrders = orderRepository.findScheduledOrdersReadyForFulfillment(now);

        if (readyOrders.isEmpty()) {
            return;
        }

        logger.info("Found {} orders ready for fulfillment", readyOrders.size());

        TriggerContext triggerContext = TriggerContext.scheduled("FulfillmentScheduler");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        readyOrders.forEach(
                order -> {
                    try {
                        logger.info(
                                "Initiating fulfillment for order: {} (scheduled pickup: {}, lead"
                                        + " time: {} minutes)",
                                order.getOrderId(),
                                order.getScheduledPickupTime().getPickupTime(),
                                order.getFulfillmentLeadTime().getMinutes());

                        orderApplicationService.initiateFulfillment(
                                new InitiateFulfillmentCommand(order.getOrderId()), triggerContext);

                        successCount.incrementAndGet();
                        logger.info(
                                "Successfully initiated fulfillment for order: {}",
                                order.getOrderId());

                    } catch (IllegalStateException e) {
                        failureCount.incrementAndGet();
                        logger.warn(
                                "Failed to initiate fulfillment for order: {} - Invalid state: {}",
                                order.getOrderId(),
                                e.getMessage());

                    } catch (IllegalArgumentException e) {
                        failureCount.incrementAndGet();
                        logger.error(
                                "Failed to initiate fulfillment for order: {} - Order not found:"
                                        + " {}",
                                order.getOrderId(),
                                e.getMessage());

                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        logger.error(
                                "Failed to initiate fulfillment for order: {} - Unexpected error",
                                order.getOrderId(),
                                e);
                    }
                });

        logger.info(
                "Fulfillment initiation completed: {} successful, {} failed, {} total",
                successCount.get(),
                failureCount.get(),
                readyOrders.size());
    }
}
