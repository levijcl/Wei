package com.wei.orchestrator.unit.order.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.infrastructure.scheduler.FulfillmentScheduler;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

@ExtendWith(MockitoExtension.class)
class FulfillmentSchedulerTest {
    @Mock private OrderApplicationService orderApplicationService;
    @Mock private OrderRepository orderRepository;
    @Mock private LockRegistry lockRegistry;
    @Mock private Lock lock;

    @InjectMocks private FulfillmentScheduler fulfillmentScheduler;

    private static final String LOCK_KEY = "order-fulfillment-initiation";

    @Test
    void shouldAcquireLockAndInitiateFulfillmentSuccessfully() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(List.of(prepareScheduledOrder("ORDER-001")));

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService)
                .initiateFulfillment(argThat(cmd -> cmd.getOrderId().equals("ORDER-001")), any());
        verify(lock).unlock();
    }

    @Test
    void shouldSkipInitiateFulfillmentIfNotFoundAnyOrder() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(List.of());

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, never())
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    @Test
    void shouldSkipProcessWhenNotAcquired() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(false);

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository, never())
                .findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, never())
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock, never()).unlock();
    }

    @Test
    void shouldReleaseLockWhenFails() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(List.of(prepareScheduledOrder("ORDER-002")));
        doThrow(new RuntimeException("Failed"))
                .when(orderApplicationService)
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService)
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    @Test
    void shouldHandleInterruptedExceptionDuringLockAcquisition() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository, never())
                .findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, never())
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock, never()).unlock();
    }

    @Test
    void shouldHandleExceptionDuringLockRelease() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(List.of(prepareScheduledOrder("ORDER-003")));
        doThrow(new RuntimeException("Unlock failed")).when(lock).unlock();

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService)
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    @Test
    void shouldProcessMultipleOrdersSuccessfully() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(
                        List.of(
                                prepareScheduledOrder("ORDER-004"),
                                prepareScheduledOrder("ORDER-005"),
                                prepareScheduledOrder("ORDER-006")));

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, times(3))
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    @Test
    void shouldContinueProcessingOtherOrdersWhenOneOrderFails() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(
                        List.of(
                                prepareScheduledOrder("ORDER-007"),
                                prepareScheduledOrder("ORDER-008"),
                                prepareScheduledOrder("ORDER-009")));

        doNothing()
                .doThrow(new IllegalStateException("Invalid state"))
                .doNothing()
                .when(orderApplicationService)
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, times(3))
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    @Test
    void shouldHandleDifferentExceptionTypesGracefully() throws InterruptedException {
        when(lockRegistry.obtain(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
        when(orderRepository.findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class)))
                .thenReturn(
                        List.of(
                                prepareScheduledOrder("ORDER-010"),
                                prepareScheduledOrder("ORDER-011"),
                                prepareScheduledOrder("ORDER-012")));

        doThrow(new IllegalStateException("Invalid state"))
                .doThrow(new IllegalArgumentException("Order not found"))
                .doThrow(new RuntimeException("Unexpected error"))
                .when(orderApplicationService)
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());

        fulfillmentScheduler.initiateFulfillment();

        verify(lockRegistry).obtain(LOCK_KEY);
        verify(lock).tryLock(1, TimeUnit.SECONDS);
        verify(orderRepository).findScheduledOrdersReadyForFulfillment(any(LocalDateTime.class));
        verify(orderApplicationService, times(3))
                .initiateFulfillment(any(InitiateFulfillmentCommand.class), any());
        verify(lock).unlock();
    }

    private Order prepareScheduledOrder(String orderId) {
        LocalDateTime now = LocalDateTime.now();
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));

        Order order = new Order(orderId, items);
        order.setFulfillmentLeadTime(FulfillmentLeadTime.defaultLeadTime());
        order.setScheduledPickupTime(new ScheduledPickupTime(now.plusHours(2)));
        order.setStatus(OrderStatus.SCHEDULED);

        return order;
    }
}
