package com.wei.orchestrator.integration.order.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.ShipmentInfo;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.infrastructure.repository.OrderRepositoryImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import(OrderRepositoryImpl.class)
class OrderRepositoryIntegrationTest {

    @Autowired private OrderRepositoryImpl orderRepository;

    @Nested
    class BasicCrudOperations {

        @Test
        void shouldSaveAndFindOrderById() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
            items.add(new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));

            Order order = new Order("ORDER-001", items);

            Order savedOrder = orderRepository.save(order);

            assertNotNull(savedOrder);
            assertEquals("ORDER-001", savedOrder.getOrderId());
            assertEquals(OrderStatus.CREATED, savedOrder.getStatus());
            assertEquals(2, savedOrder.getOrderLineItems().size());
        }

        @Test
        void shouldFindOrderByIdAfterSaving() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-100", 3, new BigDecimal("30.00")));

            Order order = new Order("ORDER-002", items);
            orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-002");

            assertTrue(foundOrder.isPresent());
            assertEquals("ORDER-002", foundOrder.get().getOrderId());
            assertEquals(1, foundOrder.get().getOrderLineItems().size());
            assertEquals("SKU-100", foundOrder.get().getOrderLineItems().get(0).getSku());
        }

        @Test
        void shouldReturnEmptyWhenOrderNotFound() {
            Optional<Order> foundOrder = orderRepository.findById("NON-EXISTENT");

            assertFalse(foundOrder.isPresent());
        }

        @Test
        void shouldUpdateExistingOrder() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-400", 4, new BigDecimal("40.00")));

            Order order = new Order("ORDER-005", items);
            orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-005");
            assertTrue(foundOrder.isPresent());

            Order existingOrder = foundOrder.get();
            String lineItemId = existingOrder.getOrderLineItems().get(0).getLineItemId();
            existingOrder.reserveLineItem(lineItemId, "TX-002", "EXT-RES-002", "WH-002");

            Order updatedOrder = orderRepository.save(existingOrder);

            Optional<Order> reloadedOrder = orderRepository.findById("ORDER-005");
            assertTrue(reloadedOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, reloadedOrder.get().getStatus());
            assertEquals(
                    "WH-002",
                    reloadedOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getReservationInfo()
                            .getWarehouseId());
        }

        @Test
        void shouldDeleteOrderById() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-500", 8, new BigDecimal("80.00")));

            Order order = new Order("ORDER-006", items);
            orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-006");
            assertTrue(foundOrder.isPresent());

            orderRepository.deleteById("ORDER-006");

            Optional<Order> deletedOrder = orderRepository.findById("ORDER-006");
            assertFalse(deletedOrder.isPresent());
        }

        @Test
        void shouldCascadeDeleteOrderLineItems() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-600", 1, new BigDecimal("10.00")));
            items.add(new OrderLineItem("SKU-601", 2, new BigDecimal("20.00")));
            items.add(new OrderLineItem("SKU-602", 3, new BigDecimal("30.00")));

            Order order = new Order("ORDER-007", items);
            orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-007");
            assertTrue(foundOrder.isPresent());
            assertEquals(3, foundOrder.get().getOrderLineItems().size());

            orderRepository.deleteById("ORDER-007");

            Optional<Order> deletedOrder = orderRepository.findById("ORDER-007");
            assertFalse(deletedOrder.isPresent());
        }
    }

    @Nested
    class ReservationPersistenceTests {

        @Test
        void shouldSaveOrderWithReservationInfo() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00")));

            Order order = new Order("ORDER-003", items);
            String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
            order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");

            Order savedOrder = orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-003");
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, foundOrder.get().getStatus());
            assertTrue(foundOrder.get().getOrderLineItems().get(0).isReserved());
            assertNotNull(foundOrder.get().getOrderLineItems().get(0).getReservationInfo());
            assertEquals(
                    "WH-001",
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getReservationInfo()
                            .getWarehouseId());
            assertEquals(
                    "EXT-RES-001",
                    foundOrder
                            .get()
                            .getOrderLineItems()
                            .get(0)
                            .getReservationInfo()
                            .getExternalReservationId());
        }
    }

    @Nested
    class ShipmentPersistenceTests {

        @Test
        void shouldSaveOrderWithShipmentInfo() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-300", 2, new BigDecimal("20.00")));

            Order order = new Order("ORDER-004", items);
            order.setStatus(OrderStatus.RESERVED);
            order.commitOrder();
            ShipmentInfo shipmentInfo = new ShipmentInfo("DHL", "TRACK-12345");
            order.markAsShipped(shipmentInfo);

            Order savedOrder = orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-004");
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.SHIPPED, foundOrder.get().getStatus());
            assertNotNull(foundOrder.get().getShipmentInfo());
            assertEquals("DHL", foundOrder.get().getShipmentInfo().getCarrier());
            assertEquals("TRACK-12345", foundOrder.get().getShipmentInfo().getTrackingNumber());
        }
    }

    @Nested
    class SchedulingPersistenceTests {

        @Test
        void shouldSaveOrderWithScheduledPickupTimeAndFulfillmentLeadTime() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-800", 10, new BigDecimal("100.00")));

            Order order = new Order("ORDER-009", items);
            LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
            FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);
            order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);

            Order savedOrder = orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-009");
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.SCHEDULED, foundOrder.get().getStatus());
            assertNotNull(foundOrder.get().getScheduledPickupTime());
            assertNotNull(foundOrder.get().getFulfillmentLeadTime());
            assertEquals(pickupTime, foundOrder.get().getScheduledPickupTime().getPickupTime());
            assertEquals(120, foundOrder.get().getFulfillmentLeadTime().getMinutes());
        }

        @Test
        void shouldSaveAndRetrieveOrderWithNullScheduledPickupTime() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-810", 5, new BigDecimal("50.00")));

            Order order = new Order("ORDER-010", items);
            Order savedOrder = orderRepository.save(order);

            Optional<Order> foundOrder = orderRepository.findById("ORDER-010");
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.CREATED, foundOrder.get().getStatus());
            assertNull(foundOrder.get().getScheduledPickupTime());
            assertNull(foundOrder.get().getFulfillmentLeadTime());
        }

        @Test
        void shouldUpdateScheduledOrderToAwaitingFulfillment() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-1600", 5, new BigDecimal("50.00")));

            Order order = new Order("ORDER-023", items);
            LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 16, 0);
            order.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime), FulfillmentLeadTime.ofHours(1));
            orderRepository.save(order);

            Optional<Order> scheduledOrder = orderRepository.findById("ORDER-023");
            assertTrue(scheduledOrder.isPresent());
            assertEquals(OrderStatus.SCHEDULED, scheduledOrder.get().getStatus());

            Order orderToUpdate = scheduledOrder.get();
            orderToUpdate.markReadyForFulfillment();
            orderRepository.save(orderToUpdate);

            Optional<Order> updatedOrder = orderRepository.findById("ORDER-023");
            assertTrue(updatedOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, updatedOrder.get().getStatus());
            assertNotNull(updatedOrder.get().getScheduledPickupTime());
            assertNotNull(updatedOrder.get().getFulfillmentLeadTime());
        }
    }

    @Nested
    class QueryOperationsTests {

        @Test
        void shouldFindOrdersByStatus() {
            List<OrderLineItem> items1 = new ArrayList<>();
            items1.add(new OrderLineItem("SKU-900", 5, new BigDecimal("50.00")));
            Order order1 = new Order("ORDER-011", items1);
            orderRepository.save(order1);

            List<OrderLineItem> items2 = new ArrayList<>();
            items2.add(new OrderLineItem("SKU-901", 3, new BigDecimal("30.00")));
            Order order2 = new Order("ORDER-012", items2);
            LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
            order2.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime), FulfillmentLeadTime.ofHours(2));
            orderRepository.save(order2);

            List<OrderLineItem> items3 = new ArrayList<>();
            items3.add(new OrderLineItem("SKU-902", 2, new BigDecimal("20.00")));
            Order order3 = new Order("ORDER-013", items3);
            LocalDateTime pickupTime2 = LocalDateTime.of(2025, 11, 6, 15, 0);
            order3.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime2), FulfillmentLeadTime.ofHours(1));
            orderRepository.save(order3);

            List<Order> createdOrders = orderRepository.findByStatus(OrderStatus.CREATED);
            List<Order> scheduledOrders = orderRepository.findByStatus(OrderStatus.SCHEDULED);

            assertTrue(createdOrders.size() >= 1);
            assertTrue(scheduledOrders.size() >= 2);

            boolean foundOrder1 =
                    createdOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-011"));
            assertTrue(foundOrder1);

            boolean foundOrder2 =
                    scheduledOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-012"));
            boolean foundOrder3 =
                    scheduledOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-013"));
            assertTrue(foundOrder2);
            assertTrue(foundOrder3);
        }

        @Test
        void shouldReturnEmptyListWhenNoOrdersWithSpecifiedStatus() {
            List<Order> failedOrders = orderRepository.findByStatus(OrderStatus.FAILED_TO_RESERVE);

            assertNotNull(failedOrders);
            assertTrue(
                    failedOrders.stream()
                            .noneMatch(o -> o.getStatus() == OrderStatus.FAILED_TO_RESERVE));
        }

        @Test
        void shouldFindScheduledOrdersReadyForFulfillment() {
            LocalDateTime now = LocalDateTime.now();

            List<OrderLineItem> items1 = new ArrayList<>();
            items1.add(new OrderLineItem("SKU-1000", 5, new BigDecimal("50.00")));
            Order order1 = new Order("ORDER-014", items1);
            LocalDateTime pastPickupTime = now.plusHours(1);
            order1.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pastPickupTime), FulfillmentLeadTime.ofMinutes(30));
            orderRepository.save(order1);

            List<OrderLineItem> items2 = new ArrayList<>();
            items2.add(new OrderLineItem("SKU-1001", 3, new BigDecimal("30.00")));
            Order order2 = new Order("ORDER-015", items2);
            LocalDateTime futurePickupTime = now.plusHours(5);
            order2.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(futurePickupTime), FulfillmentLeadTime.ofHours(2));
            orderRepository.save(order2);

            LocalDateTime currentTime = now.plusMinutes(35);
            List<Order> readyOrders =
                    orderRepository.findScheduledOrdersReadyForFulfillment(currentTime);

            assertNotNull(readyOrders);
            assertTrue(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-014")));
            assertFalse(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-015")));
        }

        @Test
        void shouldNotFindScheduledOrdersWhenCurrentTimeIsBeforeFulfillmentTime() {
            LocalDateTime now = LocalDateTime.now();

            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-1100", 5, new BigDecimal("50.00")));
            Order order = new Order("ORDER-016", items);
            LocalDateTime pickupTime = now.plusHours(3);
            order.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime), FulfillmentLeadTime.ofHours(2));
            orderRepository.save(order);

            LocalDateTime currentTime = now;
            List<Order> readyOrders =
                    orderRepository.findScheduledOrdersReadyForFulfillment(currentTime);

            assertNotNull(readyOrders);
            assertFalse(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-016")));
        }

        @Test
        void shouldFindMultipleScheduledOrdersReadyForFulfillment() {
            LocalDateTime now = LocalDateTime.now();

            List<OrderLineItem> items1 = new ArrayList<>();
            items1.add(new OrderLineItem("SKU-1200", 5, new BigDecimal("50.00")));
            Order order1 = new Order("ORDER-017", items1);
            LocalDateTime pickupTime1 = now.plusHours(1);
            order1.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime1), FulfillmentLeadTime.ofMinutes(30));
            orderRepository.save(order1);

            List<OrderLineItem> items2 = new ArrayList<>();
            items2.add(new OrderLineItem("SKU-1201", 3, new BigDecimal("30.00")));
            Order order2 = new Order("ORDER-018", items2);
            LocalDateTime pickupTime2 = now.plusMinutes(90);
            order2.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime2), FulfillmentLeadTime.ofMinutes(45));
            orderRepository.save(order2);

            LocalDateTime currentTime = now.plusMinutes(50);
            List<Order> readyOrders =
                    orderRepository.findScheduledOrdersReadyForFulfillment(currentTime);

            assertNotNull(readyOrders);
            assertTrue(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-017")));
            assertTrue(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-018")));
        }

        @Test
        void shouldNotReturnNonScheduledOrders() {
            LocalDateTime now = LocalDateTime.now();

            List<OrderLineItem> items1 = new ArrayList<>();
            items1.add(new OrderLineItem("SKU-1300", 5, new BigDecimal("50.00")));
            Order order1 = new Order("ORDER-019", items1);
            orderRepository.save(order1);

            List<OrderLineItem> items2 = new ArrayList<>();
            items2.add(new OrderLineItem("SKU-1301", 3, new BigDecimal("30.00")));
            Order order2 = new Order("ORDER-020", items2);
            order2.markReadyForFulfillment();
            orderRepository.save(order2);

            LocalDateTime currentTime = now;
            List<Order> readyOrders =
                    orderRepository.findScheduledOrdersReadyForFulfillment(currentTime);

            assertNotNull(readyOrders);
            assertFalse(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-019")));
            assertFalse(readyOrders.stream().anyMatch(o -> o.getOrderId().equals("ORDER-020")));
        }
    }

    @Nested
    class OrderLifecycleTests {

        @Test
        void shouldSaveCompleteOrderLifecycle() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-700", 15, new BigDecimal("150.00")));

            Order order = new Order("ORDER-008", items);
            orderRepository.save(order);

            Optional<Order> createdOrder = orderRepository.findById("ORDER-008");
            assertTrue(createdOrder.isPresent());
            assertEquals(OrderStatus.CREATED, createdOrder.get().getStatus());

            Order orderToReserve = createdOrder.get();
            String lineItemId = orderToReserve.getOrderLineItems().get(0).getLineItemId();
            orderToReserve.reserveLineItem(lineItemId, "TX-003", "EXT-RES-003", "WH-003");
            orderRepository.save(orderToReserve);

            Optional<Order> reservedOrder = orderRepository.findById("ORDER-008");
            assertTrue(reservedOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, reservedOrder.get().getStatus());

            Order orderToCommit = reservedOrder.get();
            orderToCommit.commitOrder();
            orderRepository.save(orderToCommit);

            Optional<Order> committedOrder = orderRepository.findById("ORDER-008");
            assertTrue(committedOrder.isPresent());
            assertEquals(OrderStatus.COMMITTED, committedOrder.get().getStatus());

            Order orderToShip = committedOrder.get();
            ShipmentInfo shipmentInfo = new ShipmentInfo("FedEx", "TRACK-99999");
            orderToShip.markAsShipped(shipmentInfo);
            orderRepository.save(orderToShip);

            Optional<Order> shippedOrder = orderRepository.findById("ORDER-008");
            assertTrue(shippedOrder.isPresent());
            assertEquals(OrderStatus.SHIPPED, shippedOrder.get().getStatus());
            assertEquals("FedEx", shippedOrder.get().getShipmentInfo().getCarrier());
        }

        @Test
        void shouldSaveCompleteScheduledOrderLifecycle() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-1400", 10, new BigDecimal("100.00")));

            Order order = new Order("ORDER-021", items);
            orderRepository.save(order);

            Optional<Order> createdOrder = orderRepository.findById("ORDER-021");
            assertTrue(createdOrder.isPresent());
            assertEquals(OrderStatus.CREATED, createdOrder.get().getStatus());

            Order orderToSchedule = createdOrder.get();
            LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
            orderToSchedule.scheduleForLaterFulfillment(
                    new ScheduledPickupTime(pickupTime), FulfillmentLeadTime.ofHours(2));
            orderRepository.save(orderToSchedule);

            Optional<Order> scheduledOrder = orderRepository.findById("ORDER-021");
            assertTrue(scheduledOrder.isPresent());
            assertEquals(OrderStatus.SCHEDULED, scheduledOrder.get().getStatus());
            assertNotNull(scheduledOrder.get().getScheduledPickupTime());
            assertNotNull(scheduledOrder.get().getFulfillmentLeadTime());

            Order orderToMarkReady = scheduledOrder.get();
            orderToMarkReady.markReadyForFulfillment();
            orderRepository.save(orderToMarkReady);

            Optional<Order> awaitingOrder = orderRepository.findById("ORDER-021");
            assertTrue(awaitingOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, awaitingOrder.get().getStatus());

            Order orderToReserve = awaitingOrder.get();
            String lineItemId = orderToReserve.getOrderLineItems().get(0).getLineItemId();
            orderToReserve.reserveLineItem(lineItemId, "TX-004", "EXT-RES-004", "WH-001");
            orderRepository.save(orderToReserve);

            Optional<Order> reservedOrder = orderRepository.findById("ORDER-021");
            assertTrue(reservedOrder.isPresent());
            assertEquals(OrderStatus.RESERVED, reservedOrder.get().getStatus());

            Order orderToCommit = reservedOrder.get();
            orderToCommit.commitOrder();
            orderRepository.save(orderToCommit);

            Optional<Order> committedOrder = orderRepository.findById("ORDER-021");
            assertTrue(committedOrder.isPresent());
            assertEquals(OrderStatus.COMMITTED, committedOrder.get().getStatus());

            Order orderToShip = committedOrder.get();
            ShipmentInfo shipmentInfo = new ShipmentInfo("UPS", "TRACK-54321");
            orderToShip.markAsShipped(shipmentInfo);
            orderRepository.save(orderToShip);

            Optional<Order> shippedOrder = orderRepository.findById("ORDER-021");
            assertTrue(shippedOrder.isPresent());
            assertEquals(OrderStatus.SHIPPED, shippedOrder.get().getStatus());
            assertEquals("UPS", shippedOrder.get().getShipmentInfo().getCarrier());
        }

        @Test
        void shouldSaveFailedReservationScenario() {
            List<OrderLineItem> items = new ArrayList<>();
            items.add(new OrderLineItem("SKU-1500", 10, new BigDecimal("100.00")));

            Order order = new Order("ORDER-022", items);
            order.markReadyForFulfillment();
            orderRepository.save(order);

            Optional<Order> awaitingOrder = orderRepository.findById("ORDER-022");
            assertTrue(awaitingOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, awaitingOrder.get().getStatus());

            Order orderToFail = awaitingOrder.get();
            orderToFail.markAsFailedToReserve("Insufficient inventory");
            orderRepository.save(orderToFail);

            Optional<Order> failedOrder = orderRepository.findById("ORDER-022");
            assertTrue(failedOrder.isPresent());
            assertEquals(OrderStatus.FAILED_TO_RESERVE, failedOrder.get().getStatus());
        }
    }
}
