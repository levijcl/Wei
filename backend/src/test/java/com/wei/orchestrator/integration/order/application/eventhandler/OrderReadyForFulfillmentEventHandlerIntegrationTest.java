package com.wei.orchestrator.integration.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.wei.orchestrator.inventory.application.InventoryApplicationService;
import com.wei.orchestrator.inventory.application.dto.InventoryOperationResultDto;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.application.eventhandler.OrderReadyForFulfillmentEventHandler;
import com.wei.orchestrator.order.domain.event.OrderReadyForFulfillmentEvent;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class OrderReadyForFulfillmentEventHandlerIntegrationTest {

    @Autowired private ApplicationEventPublisher eventPublisher;

    @Autowired private OrderRepository orderRepository;

    @Autowired private OrderReadyForFulfillmentEventHandler eventHandler;

    @Autowired private OrderApplicationService orderApplicationService;

    @Autowired private TransactionTemplate transactionTemplate;

    @MockitoBean private InventoryApplicationService inventoryApplicationService;

    @Nested
    class EventPublicationAndHandling {

        @Test
        void shouldHandleOrderReadyForFulfillmentEvent() {
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-001", 10, new BigDecimal("100.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            eventHandler.handleOrderReadyForFulfillment(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, foundOrder.get().getStatus());
        }

        @Test
        void shouldReserveInventoryForOrderLineItems() {
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(
                                    new OrderLineItem("SKU-100", 5, new BigDecimal("50.00")),
                                    new OrderLineItem("SKU-101", 3, new BigDecimal("30.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            eventHandler.handleOrderReadyForFulfillment(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
        }

        @Test
        void shouldDirectlyInvokeHandlerMethod() {
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            Order order =
                    new Order(
                            orderId,
                            List.of(new OrderLineItem("SKU-200", 7, new BigDecimal("70.00"))));
            order.createOrder();
            order.markReadyForFulfillment();
            orderRepository.save(order);

            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            eventHandler.handleOrderReadyForFulfillment(event);

            Optional<Order> foundOrder = orderRepository.findById(orderId);
            assertTrue(foundOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, foundOrder.get().getStatus());
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            OrderReadyForFulfillmentEvent event = new OrderReadyForFulfillmentEvent(orderId);

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () -> {
                                eventHandler.handleOrderReadyForFulfillment(event);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
        }
    }

    @Nested
    class IntegrationWithOrderApplicationService {

        @Test
        void shouldHandleEventWhenInitiateFulfillmentIsCalled() {
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            String orderId = "INT-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            List<CreateOrderCommand.OrderLineItemDto> items =
                    List.of(
                            new CreateOrderCommand.OrderLineItemDto(
                                    "SKU-300", 2, new BigDecimal("20.00")));

            CreateOrderCommand createCommand = new CreateOrderCommand(orderId, items);
            orderApplicationService.createOrder(createCommand, TriggerContext.manual());

            InitiateFulfillmentCommand fulfillmentCommand = new InitiateFulfillmentCommand(orderId);
            orderApplicationService.initiateFulfillment(fulfillmentCommand);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, order.get().getStatus());
        }
    }

    @Nested
    class TransactionIsolation {

        @Test
        void shouldNotRollbackInitiateFulfillmentWhenHandlerFailsAfterCommit() {
            String orderId = "CALLER-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<CreateOrderCommand.OrderLineItemDto> items =
                                List.of(
                                        new CreateOrderCommand.OrderLineItemDto(
                                                "SKU-400", 1, new BigDecimal("10.00")));
                        CreateOrderCommand createCommand = new CreateOrderCommand(orderId, items);
                        orderApplicationService.createOrder(createCommand, TriggerContext.manual());
                        return null;
                    });

            doThrow(new RuntimeException("Inventory system down"))
                    .when(inventoryApplicationService)
                    .reserveInventory(any());

            InitiateFulfillmentCommand fulfillmentCommand = new InitiateFulfillmentCommand(orderId);

            orderApplicationService.initiateFulfillment(fulfillmentCommand);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(
                    order.isPresent(),
                    "Order should still exist (created in separate transaction)");
            assertEquals(
                    OrderStatus.AWAITING_FULFILLMENT,
                    order.get().getStatus(),
                    "Order status should be AWAITING_FULFILLMENT (handler runs after commit, so"
                            + " initiateFulfillment already committed)");
        }

        @Test
        void shouldCommitBothTransactionsWhenHandlerSucceeds() {
            when(inventoryApplicationService.reserveInventory(any()))
                    .thenReturn(InventoryOperationResultDto.success("reservation-1"));

            String orderId = "SUCCESS-ORDER-" + UUID.randomUUID().toString().substring(0, 8);

            transactionTemplate.execute(
                    status -> {
                        List<CreateOrderCommand.OrderLineItemDto> items =
                                List.of(
                                        new CreateOrderCommand.OrderLineItemDto(
                                                "SKU-500", 2, new BigDecimal("20.00")));
                        CreateOrderCommand createCommand = new CreateOrderCommand(orderId, items);
                        orderApplicationService.createOrder(createCommand, TriggerContext.manual());
                        return null;
                    });

            InitiateFulfillmentCommand fulfillmentCommand = new InitiateFulfillmentCommand(orderId);
            orderApplicationService.initiateFulfillment(fulfillmentCommand);

            Optional<Order> order = orderRepository.findById(orderId);
            assertTrue(order.isPresent(), "Order should exist");
            assertEquals(
                    OrderStatus.AWAITING_FULFILLMENT,
                    order.get().getStatus(),
                    "Order status should be AWAITING_FULFILLMENT");
        }
    }
}
