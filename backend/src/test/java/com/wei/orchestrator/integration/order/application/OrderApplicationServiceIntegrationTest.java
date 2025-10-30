package com.wei.orchestrator.integration.order.application;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.integration.BaseIntegrationTest;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class OrderApplicationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private OrderApplicationService orderApplicationService;

    @Autowired private OrderRepository orderRepository;

    @Test
    void shouldCreateOrderAndPersistToDatabase() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-002", 5, new BigDecimal("50.00")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

        Order createdOrder = orderApplicationService.createOrder(command);

        assertNotNull(createdOrder);
        assertEquals("ORDER-001", createdOrder.getOrderId());
        assertEquals(OrderStatus.CREATED, createdOrder.getStatus());
        assertEquals(2, createdOrder.getOrderLineItems().size());

        Optional<Order> foundOrder = orderRepository.findById("ORDER-001");
        assertTrue(foundOrder.isPresent());
        assertEquals("ORDER-001", foundOrder.get().getOrderId());
        assertEquals(2, foundOrder.get().getOrderLineItems().size());
    }

    @Test
    void shouldCreateOrderWithMultipleLineItems() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-100", 3, new BigDecimal("30.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-101", 7, new BigDecimal("70.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-102", 2, new BigDecimal("20.00")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-002", items);

        Order createdOrder = orderApplicationService.createOrder(command);

        assertEquals(3, createdOrder.getOrderLineItems().size());
        assertEquals("SKU-100", createdOrder.getOrderLineItems().get(0).getSku());
        assertEquals(3, createdOrder.getOrderLineItems().get(0).getQuantity());
        assertEquals(new BigDecimal("30.00"), createdOrder.getOrderLineItems().get(0).getPrice());

        Optional<Order> persistedOrder = orderRepository.findById("ORDER-002");
        assertTrue(persistedOrder.isPresent());
        assertEquals(3, persistedOrder.get().getOrderLineItems().size());
    }

    @Test
    void shouldPersistLineItemsCorrectly() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-200", 15, new BigDecimal("150.50")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-004", items);

        orderApplicationService.createOrder(command);

        Optional<Order> persistedOrder = orderRepository.findById("ORDER-004");
        assertTrue(persistedOrder.isPresent());

        Order order = persistedOrder.get();
        assertEquals(1, order.getOrderLineItems().size());
        assertEquals("SKU-200", order.getOrderLineItems().get(0).getSku());
        assertEquals(15, order.getOrderLineItems().get(0).getQuantity());
        assertEquals(new BigDecimal("150.50"), order.getOrderLineItems().get(0).getPrice());
    }

    @Test
    void shouldCreateMultipleOrdersIndependently() {
        List<CreateOrderCommand.OrderLineItemDto> items1 = new ArrayList<>();
        items1.add(new CreateOrderCommand.OrderLineItemDto("SKU-500", 1, new BigDecimal("10.00")));
        CreateOrderCommand command1 = new CreateOrderCommand("ORDER-005", items1);

        List<CreateOrderCommand.OrderLineItemDto> items2 = new ArrayList<>();
        items2.add(new CreateOrderCommand.OrderLineItemDto("SKU-501", 2, new BigDecimal("20.00")));
        CreateOrderCommand command2 = new CreateOrderCommand("ORDER-006", items2);

        Order order1 = orderApplicationService.createOrder(command1);
        Order order2 = orderApplicationService.createOrder(command2);

        assertNotNull(order1);
        assertNotNull(order2);
        assertNotEquals(order1.getOrderId(), order2.getOrderId());

        Optional<Order> persistedOrder1 = orderRepository.findById("ORDER-005");
        Optional<Order> persistedOrder2 = orderRepository.findById("ORDER-006");

        assertTrue(persistedOrder1.isPresent());
        assertTrue(persistedOrder2.isPresent());
    }

    @Test
    void shouldHandleDecimalPricesCorrectly() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-300", 1, new BigDecimal("99.99")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-301", 2, new BigDecimal("0.01")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-007", items);

        orderApplicationService.createOrder(command);

        Optional<Order> persistedOrder = orderRepository.findById("ORDER-007");
        assertTrue(persistedOrder.isPresent());

        Order order = persistedOrder.get();
        assertEquals(new BigDecimal("99.99"), order.getOrderLineItems().get(0).getPrice());
        assertEquals(new BigDecimal("0.01"), order.getOrderLineItems().get(1).getPrice());
    }
}
