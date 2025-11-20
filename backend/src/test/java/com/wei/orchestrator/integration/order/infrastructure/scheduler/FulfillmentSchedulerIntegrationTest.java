package com.wei.orchestrator.integration.order.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.eventhandler.OrderReadyForFulfillmentEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.infrastructure.scheduler.FulfillmentScheduler;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class FulfillmentSchedulerIntegrationTest {

    @Autowired private FulfillmentScheduler fulfillmentScheduler;

    @Autowired private OrderApplicationService orderApplicationService;

    @Autowired private OrderRepository orderRepository;

    @Autowired private LockRegistry lockRegistry;

    @Autowired private DataSource dataSource;

    @MockitoBean private OrderReadyForFulfillmentEventHandler orderReadyForFulfillmentEventHandler;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        clearLockTable();
        clearOrderData();
    }

    private void clearLockTable() {
        jdbcTemplate.execute("DELETE FROM int_lock");
    }

    private void clearOrderData() {
        jdbcTemplate.execute("DELETE FROM order_line_item");
        jdbcTemplate.execute("DELETE FROM orders");
    }

    private int countLocksInDatabase() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM int_lock", Integer.class);
        return count != null ? count : 0;
    }

    @Test
    void shouldExecuteScheduledFulfillmentInitiationWithRealLocks() {
        LocalDateTime futurePickupTime = LocalDateTime.now().plusMinutes(30);
        Duration leadTime = Duration.ofMinutes(90);
        createScheduledOrder("ORDER-INT-001", futurePickupTime, leadTime);

        fulfillmentScheduler.initiateFulfillment();

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-001")));
    }

    @Test
    void shouldReleaseLockAfterFulfillmentInitiationCompletes() {
        fulfillmentScheduler.initiateFulfillment();

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldAllowMultipleSequentialFulfillmentCycles() {
        fulfillmentScheduler.initiateFulfillment();
        fulfillmentScheduler.initiateFulfillment();
        fulfillmentScheduler.initiateFulfillment();

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldInitiateFulfillmentForReadyOrders() {
        LocalDateTime pickupTime = LocalDateTime.now().plusMinutes(60);
        Duration leadTime = Duration.ofMinutes(90);
        createScheduledOrder("ORDER-INT-002", pickupTime, leadTime);

        fulfillmentScheduler.initiateFulfillment();

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-002")));
    }

    @Test
    void shouldNotInitiateFulfillmentForOrdersNotReady() {
        LocalDateTime futurePickupTime = LocalDateTime.now().plusHours(5);
        Duration leadTime = Duration.ofHours(2);
        createScheduledOrder("ORDER-INT-003", futurePickupTime, leadTime);

        fulfillmentScheduler.initiateFulfillment();

        List<Order> scheduledOrders = orderRepository.findByStatus(OrderStatus.SCHEDULED);
        assertTrue(scheduledOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-003")));

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertFalse(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-003")));
    }

    @Test
    void shouldProcessMultipleReadyOrdersInSingleCycle() {
        LocalDateTime pickupTime = LocalDateTime.now().plusMinutes(60);
        Duration leadTime = Duration.ofMinutes(90);

        createScheduledOrder("ORDER-INT-004", pickupTime, leadTime);
        createScheduledOrder("ORDER-INT-005", pickupTime, leadTime);
        createScheduledOrder("ORDER-INT-006", pickupTime, leadTime);

        fulfillmentScheduler.initiateFulfillment();

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-004")));
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-005")));
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-006")));
    }

    @Test
    void shouldAllowSecondInitiationAfterFirstCompletes() throws InterruptedException {
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);

        Thread thread1 =
                new Thread(
                        () -> {
                            fulfillmentScheduler.initiateFulfillment();
                            executionCount.incrementAndGet();
                            firstDone.countDown();
                        });

        Thread thread2 =
                new Thread(
                        () -> {
                            try {
                                firstDone.await();
                                Thread.sleep(50);
                                fulfillmentScheduler.initiateFulfillment();
                                executionCount.incrementAndGet();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                secondDone.countDown();
                            }
                        });

        thread1.start();
        thread2.start();

        assertTrue(secondDone.await(5, TimeUnit.SECONDS));

        assertEquals(2, executionCount.get());
        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldCleanUpAllLocksAfterInitiationCycles() throws InterruptedException {
        fulfillmentScheduler.initiateFulfillment();
        Thread.sleep(100);

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldObtainLockFromRegistry() throws InterruptedException {
        var lock = lockRegistry.obtain("test-lock");
        assertNotNull(lock);

        boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
        assertTrue(acquired);

        lock.unlock();
    }

    @Test
    void shouldReturnSameLockInstanceForSameKey() {
        var lock1 = lockRegistry.obtain("order-fulfillment-initiation");
        var lock2 = lockRegistry.obtain("order-fulfillment-initiation");

        assertSame(lock1, lock2);
    }

    @Test
    void shouldAllowDifferentLockKeysConcurrently() throws InterruptedException {
        var lock1 = lockRegistry.obtain("key1");
        var lock2 = lockRegistry.obtain("key2");

        boolean acquired1 = lock1.tryLock(1, TimeUnit.SECONDS);
        boolean acquired2 = lock2.tryLock(1, TimeUnit.SECONDS);

        assertTrue(acquired1);
        assertTrue(acquired2);

        lock1.unlock();
        lock2.unlock();
    }

    @Test
    void shouldHandleOrderWithInvalidState() {
        LocalDateTime pickupTime = LocalDateTime.now().plusMinutes(60);
        Duration leadTime = Duration.ofMinutes(90);
        Order order = createScheduledOrder("ORDER-INT-007", pickupTime, leadTime);

        order.setStatus(OrderStatus.RESERVED);
        orderRepository.save(order);

        fulfillmentScheduler.initiateFulfillment();

        Order updatedOrder = orderRepository.findById("ORDER-INT-007").orElseThrow();
        assertEquals(OrderStatus.RESERVED, updatedOrder.getStatus());
    }

    @Test
    void shouldContinueProcessingOtherOrdersWhenOneFailsValidation() {
        LocalDateTime pickupTime = LocalDateTime.now().plusMinutes(60);
        Duration leadTime = Duration.ofMinutes(90);

        Order order1 = createScheduledOrder("ORDER-INT-008", pickupTime, leadTime);
        order1.setStatus(OrderStatus.RESERVED);
        orderRepository.save(order1);

        createScheduledOrder("ORDER-INT-009", pickupTime, leadTime);
        createScheduledOrder("ORDER-INT-010", pickupTime, leadTime);

        fulfillmentScheduler.initiateFulfillment();

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-009")));
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-010")));
        assertFalse(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-008")));
    }

    @Test
    void shouldProcessOrdersWithDifferentLeadTimes() {
        LocalDateTime pickupTime = LocalDateTime.now().plusHours(2);

        createScheduledOrder("ORDER-INT-011", pickupTime, Duration.ofMinutes(90));
        createScheduledOrder("ORDER-INT-012", pickupTime, Duration.ofMinutes(150));

        fulfillmentScheduler.initiateFulfillment();

        List<Order> awaitingOrders = orderRepository.findByStatus(OrderStatus.AWAITING_FULFILLMENT);
        assertFalse(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-011")));
        assertTrue(awaitingOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-INT-012")));
    }

    private Order createScheduledOrder(
            String orderId, LocalDateTime scheduledPickupTime, Duration leadTime) {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));

        CreateOrderCommand command =
                new CreateOrderCommand(orderId, items, scheduledPickupTime, leadTime);

        return orderApplicationService.createOrder(command, TriggerContext.manual());
    }
}
