package com.wei.orchestrator.unit.order.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.ShipmentInfo;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void shouldCreateOrderWithInitialStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));

        Order order = new Order("ORDER-001", items);

        assertEquals("ORDER-001", order.getOrderId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(1, order.getOrderLineItems().size());
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrderWithNullLineItems() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new Order("ORDER-002", null);
                        });

        assertTrue(exception.getMessage().contains("Order must have at least one line item"));
    }

    @Test
    void shouldThrowExceptionWhenCreatingOrderWithEmptyLineItems() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new Order("ORDER-003", new ArrayList<>());
                        });

        assertTrue(exception.getMessage().contains("Order must have at least one line item"));
    }

    @Test
    void shouldAddOrderLineItem() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 5, new BigDecimal("50.00")));
        Order order = new Order("ORDER-004", items);
        OrderLineItem newItem = new OrderLineItem("SKU-002", 3, new BigDecimal("30.00"));

        order.addOrderLineItem(newItem);

        assertEquals(2, order.getOrderLineItems().size());
        assertEquals("SKU-002", order.getOrderLineItems().get(1).getSku());
    }

    @Test
    void shouldReserveInventoryWhenStatusIsCreated() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-005", items);
        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();

        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertTrue(order.getOrderLineItems().get(0).isReserved());
        assertEquals(
                "WH-001", order.getOrderLineItems().get(0).getReservationInfo().getWarehouseId());
        assertEquals(
                "EXT-RES-001",
                order.getOrderLineItems().get(0).getReservationInfo().getExternalReservationId());
    }

    @Test
    void shouldThrowExceptionWhenReservingAlreadyReservedLineItem() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-006", items);
        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.reserveLineItem(lineItemId, "TX-002", "EXT-RES-002", "WH-001");
                        });

        assertTrue(exception.getMessage().contains("already reserved"));
    }

    @Test
    void shouldCommitOrderWhenStatusIsReserved() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-007", items);
        order.setStatus(OrderStatus.RESERVED);

        order.commitOrder();

        assertEquals(OrderStatus.COMMITTED, order.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenCommittingOrderWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-008", items);
        order.setStatus(OrderStatus.CREATED);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.commitOrder();
                        });

        assertTrue(exception.getMessage().contains("Cannot commit order"));
    }

    @Test
    void shouldMarkAsShippedWhenStatusIsCommitted() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-009", items);
        order.setStatus(OrderStatus.COMMITTED);
        ShipmentInfo shipmentInfo = new ShipmentInfo("DHL", "TRACK-12345");

        order.markAsShipped(shipmentInfo);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("DHL", order.getShipmentInfo().getCarrier());
        assertEquals("TRACK-12345", order.getShipmentInfo().getTrackingNumber());
    }

    @Test
    void shouldThrowExceptionWhenMarkingAsShippedWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-010", items);
        order.setStatus(OrderStatus.CREATED);
        ShipmentInfo shipmentInfo = new ShipmentInfo("DHL", "TRACK-12345");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.markAsShipped(shipmentInfo);
                        });

        assertTrue(exception.getMessage().contains("Cannot mark order as shipped"));
    }

    @Test
    void shouldMaintainOrderLifecycleFromCreatedToShipped() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));

        Order order = new Order("ORDER-010", items);
        assertEquals(OrderStatus.CREATED, order.getStatus());

        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
        assertEquals(OrderStatus.RESERVED, order.getStatus());

        order.commitOrder();
        assertEquals(OrderStatus.COMMITTED, order.getStatus());

        ShipmentInfo shipmentInfo = new ShipmentInfo("UPS", "TRACK-67890");
        order.markAsShipped(shipmentInfo);
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void shouldReturnImmutableCopyOfOrderLineItems() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-011", items);

        List<OrderLineItem> retrievedItems = order.getOrderLineItems();
        retrievedItems.add(new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));

        assertEquals(1, order.getOrderLineItems().size());
    }

    @Test
    void shouldScheduleOrderForLaterFulfillmentWhenStatusIsCreated() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-012", items);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);

        order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);

        assertEquals(OrderStatus.SCHEDULED, order.getStatus());
        assertEquals(scheduledPickupTime, order.getScheduledPickupTime());
        assertEquals(leadTime, order.getFulfillmentLeadTime());
    }

    @Test
    void shouldThrowExceptionWhenSchedulingOrderWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-013", items);
        order.setStatus(OrderStatus.RESERVED);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);
                        });

        assertTrue(exception.getMessage().contains("Cannot schedule order for later fulfillment"));
    }

    @Test
    void shouldThrowExceptionWhenSchedulingOrderWithNullPickupTime() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-014", items);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            order.scheduleForLaterFulfillment(null, leadTime);
                        });

        assertTrue(exception.getMessage().contains("Scheduled pickup time cannot be null"));
    }

    @Test
    void shouldThrowExceptionWhenSchedulingOrderWithNullLeadTime() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-015", items);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            order.scheduleForLaterFulfillment(scheduledPickupTime, null);
                        });

        assertTrue(exception.getMessage().contains("Fulfillment lead time cannot be null"));
    }

    @Test
    void shouldMarkReadyForFulfillmentWhenStatusIsScheduled() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-016", items);
        order.setStatus(OrderStatus.SCHEDULED);

        order.markReadyForFulfillment();

        assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.getStatus());
    }

    @Test
    void shouldMarkReadyForFulfillmentWhenStatusIsCreated() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-017", items);

        order.markReadyForFulfillment();

        assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenMarkingReadyForFulfillmentWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-018", items);
        order.setStatus(OrderStatus.RESERVED);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.markReadyForFulfillment();
                        });

        assertTrue(exception.getMessage().contains("Cannot mark order as ready for fulfillment"));
    }

    @Test
    void shouldReturnTrueWhenOrderIsReadyForFulfillment() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-019", items);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);
        order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 12, 0);

        boolean result = order.isReadyForFulfillment(currentTime);

        assertTrue(result);
    }

    @Test
    void shouldReturnTrueWhenCurrentTimeIsAfterFulfillmentStartTime() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-020", items);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);
        order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 13, 0);

        boolean result = order.isReadyForFulfillment(currentTime);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenCurrentTimeIsBeforeFulfillmentStartTime() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-021", items);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);
        order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 11, 0);

        boolean result = order.isReadyForFulfillment(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenStatusIsNotScheduled() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-022", items);
        LocalDateTime currentTime = LocalDateTime.now();

        boolean result = order.isReadyForFulfillment(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenScheduledPickupTimeIsNull() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-023", items);
        order.setStatus(OrderStatus.SCHEDULED);
        order.setFulfillmentLeadTime(FulfillmentLeadTime.ofHours(2));
        LocalDateTime currentTime = LocalDateTime.now();

        boolean result = order.isReadyForFulfillment(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenFulfillmentLeadTimeIsNull() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-024", items);
        order.setStatus(OrderStatus.SCHEDULED);
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        order.setScheduledPickupTime(new ScheduledPickupTime(pickupTime));
        LocalDateTime currentTime = LocalDateTime.now();

        boolean result = order.isReadyForFulfillment(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldMarkAsFailedToReserveWhenStatusIsAwaitingFulfillment() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-025", items);
        order.setStatus(OrderStatus.AWAITING_FULFILLMENT);

        order.markAsFailedToReserve("Insufficient inventory");

        assertEquals(OrderStatus.FAILED_TO_RESERVE, order.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenMarkingAsFailedToReserveWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-026", items);
        order.setStatus(OrderStatus.CREATED);

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.markAsFailedToReserve("Insufficient inventory");
                        });

        assertTrue(exception.getMessage().contains("Cannot mark order as failed to reserve"));
    }

    @Test
    void shouldReserveInventoryWhenStatusIsAwaitingFulfillment() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-027", items);
        order.setStatus(OrderStatus.AWAITING_FULFILLMENT);
        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();

        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertTrue(order.getOrderLineItems().get(0).isReserved());
        assertEquals(
                "WH-001", order.getOrderLineItems().get(0).getReservationInfo().getWarehouseId());
    }

    @Test
    void shouldMaintainScheduledOrderLifecycleFromCreatedToShipped() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-028", items);
        assertEquals(OrderStatus.CREATED, order.getStatus());

        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);
        order.scheduleForLaterFulfillment(scheduledPickupTime, leadTime);
        assertEquals(OrderStatus.SCHEDULED, order.getStatus());

        order.markReadyForFulfillment();
        assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.getStatus());

        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
        assertEquals(OrderStatus.RESERVED, order.getStatus());

        order.commitOrder();
        assertEquals(OrderStatus.COMMITTED, order.getStatus());

        ShipmentInfo shipmentInfo = new ShipmentInfo("UPS", "TRACK-12345");
        order.markAsShipped(shipmentInfo);
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void shouldMaintainImmediateFulfillmentLifecycleFromCreatedToShipped() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-029", items);
        assertEquals(OrderStatus.CREATED, order.getStatus());

        order.markReadyForFulfillment();
        assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.getStatus());

        String lineItemId = order.getOrderLineItems().get(0).getLineItemId();
        order.reserveLineItem(lineItemId, "TX-001", "EXT-RES-001", "WH-001");
        assertEquals(OrderStatus.RESERVED, order.getStatus());

        order.commitOrder();
        assertEquals(OrderStatus.COMMITTED, order.getStatus());

        ShipmentInfo shipmentInfo = new ShipmentInfo("DHL", "TRACK-67890");
        order.markAsShipped(shipmentInfo);
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void shouldHandleFailedReservationScenario() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-030", items);

        order.markReadyForFulfillment();
        assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.getStatus());

        order.markAsFailedToReserve("Insufficient inventory");
        assertEquals(OrderStatus.FAILED_TO_RESERVE, order.getStatus());
    }
}
