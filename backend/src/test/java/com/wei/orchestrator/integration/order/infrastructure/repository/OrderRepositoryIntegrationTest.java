package com.wei.orchestrator.integration.order.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.ReservationInfo;
import com.wei.orchestrator.order.domain.model.ShipmentInfo;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.infrastructure.repository.OrderRepositoryImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@DataJpaTest(
        includeFilters =
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Repository.class))
class OrderRepositoryIntegrationTest {

    @Autowired private OrderRepositoryImpl orderRepository;

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
    void shouldSaveOrderWithReservationInfo() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00")));

        Order order = new Order("ORDER-003", items);
        ReservationInfo reservationInfo = new ReservationInfo("WH-001", 7, "RESERVED");
        order.reserveInventory(reservationInfo);

        Order savedOrder = orderRepository.save(order);

        Optional<Order> foundOrder = orderRepository.findById("ORDER-003");
        assertTrue(foundOrder.isPresent());
        assertEquals(OrderStatus.RESERVED, foundOrder.get().getStatus());
        assertNotNull(foundOrder.get().getReservationInfo());
        assertEquals("WH-001", foundOrder.get().getReservationInfo().getWarehouseId());
        assertEquals(7, foundOrder.get().getReservationInfo().getReservedQty());
    }

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

    @Test
    void shouldUpdateExistingOrder() {
        List<OrderLineItem> items = new ArrayList<>();
        items.add(new OrderLineItem("SKU-400", 4, new BigDecimal("40.00")));

        Order order = new Order("ORDER-005", items);
        orderRepository.save(order);

        Optional<Order> foundOrder = orderRepository.findById("ORDER-005");
        assertTrue(foundOrder.isPresent());

        Order existingOrder = foundOrder.get();
        ReservationInfo reservationInfo = new ReservationInfo("WH-002", 4, "RESERVED");
        existingOrder.reserveInventory(reservationInfo);

        Order updatedOrder = orderRepository.save(existingOrder);

        Optional<Order> reloadedOrder = orderRepository.findById("ORDER-005");
        assertTrue(reloadedOrder.isPresent());
        assertEquals(OrderStatus.RESERVED, reloadedOrder.get().getStatus());
        assertEquals("WH-002", reloadedOrder.get().getReservationInfo().getWarehouseId());
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
        ReservationInfo reservationInfo = new ReservationInfo("WH-003", 15, "RESERVED");
        orderToReserve.reserveInventory(reservationInfo);
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
}
