package com.wei.orchestrator.unit.order.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.ReservationInfo;
import com.wei.orchestrator.order.domain.model.ShipmentInfo;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import java.math.BigDecimal;
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
        ReservationInfo reservationInfo = new ReservationInfo("WH-001", 10, "RESERVED");

        order.reserveInventory(reservationInfo);

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals("WH-001", order.getReservationInfo().getWarehouseId());
        assertEquals(10, order.getReservationInfo().getReservedQty());
    }

    @Test
    void shouldThrowExceptionWhenReservingInventoryWithInvalidStatus() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")));
        Order order = new Order("ORDER-006", items);
        order.setStatus(OrderStatus.RESERVED);
        ReservationInfo reservationInfo = new ReservationInfo("WH-001", 10, "RESERVED");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> {
                            order.reserveInventory(reservationInfo);
                        });

        assertTrue(exception.getMessage().contains("Cannot reserve inventory"));
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

        ReservationInfo reservationInfo = new ReservationInfo("WH-001", 10, "RESERVED");
        order.reserveInventory(reservationInfo);
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
}
