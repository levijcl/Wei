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
    void shouldCreateOrderWithEmptyLineItems() {
        Order order = new Order("ORDER-002", null);

        assertEquals("ORDER-002", order.getOrderId());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertTrue(order.getOrderLineItems().isEmpty());
    }

    @Test
    void shouldAddOrderLineItem() {
        Order order = new Order("ORDER-003", new ArrayList<>());
        OrderLineItem item = new OrderLineItem("SKU-001", 5, new BigDecimal("50.00"));

        order.addOrderLineItem(item);

        assertEquals(1, order.getOrderLineItems().size());
        assertEquals("SKU-001", order.getOrderLineItems().get(0).getSku());
    }

    @Test
    void shouldReserveInventoryWhenStatusIsCreated() {
        Order order = new Order("ORDER-004", new ArrayList<>());
        ReservationInfo reservationInfo = new ReservationInfo("WH-001", 10, "RESERVED");

        order.reserveInventory(reservationInfo);

        assertEquals(OrderStatus.RESERVED, order.getStatus());
        assertEquals("WH-001", order.getReservationInfo().getWarehouseId());
        assertEquals(10, order.getReservationInfo().getReservedQty());
    }

    @Test
    void shouldThrowExceptionWhenReservingInventoryWithInvalidStatus() {
        Order order = new Order("ORDER-005", new ArrayList<>());
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
        Order order = new Order("ORDER-006", new ArrayList<>());
        order.setStatus(OrderStatus.RESERVED);

        order.commitOrder();

        assertEquals(OrderStatus.COMMITTED, order.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenCommittingOrderWithInvalidStatus() {
        Order order = new Order("ORDER-007", new ArrayList<>());
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
        Order order = new Order("ORDER-008", new ArrayList<>());
        order.setStatus(OrderStatus.COMMITTED);
        ShipmentInfo shipmentInfo = new ShipmentInfo("DHL", "TRACK-12345");

        order.markAsShipped(shipmentInfo);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("DHL", order.getShipmentInfo().getCarrier());
        assertEquals("TRACK-12345", order.getShipmentInfo().getTrackingNumber());
    }

    @Test
    void shouldThrowExceptionWhenMarkingAsShippedWithInvalidStatus() {
        Order order = new Order("ORDER-009", new ArrayList<>());
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
