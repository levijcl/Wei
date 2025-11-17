package com.wei.orchestrator.integration.order.application;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.application.eventhandler.OrderReadyForFulfillmentEventHandler;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationServiceIntegrationTest {

    @Autowired private OrderApplicationService orderApplicationService;

    @Autowired private OrderRepository orderRepository;

    @MockitoBean private OrderReadyForFulfillmentEventHandler orderReadyForFulfillmentEventHandler;

    @Nested
    class createOrder {

        @Test
        void shouldCreateOrderAndPersistToDatabase() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-002", 5, new BigDecimal("50.00")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

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
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-100", 3, new BigDecimal("30.00")));
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-101", 7, new BigDecimal("70.00")));
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-102", 2, new BigDecimal("20.00")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-002", items);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertEquals(3, createdOrder.getOrderLineItems().size());
            assertEquals("SKU-100", createdOrder.getOrderLineItems().get(0).getSku());
            assertEquals(3, createdOrder.getOrderLineItems().get(0).getQuantity());
            assertEquals(
                    new BigDecimal("30.00"), createdOrder.getOrderLineItems().get(0).getPrice());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-002");
            assertTrue(persistedOrder.isPresent());
            assertEquals(3, persistedOrder.get().getOrderLineItems().size());
        }

        @Test
        void shouldPersistLineItemsCorrectly() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-200", 15, new BigDecimal("150.50")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-004", items);

            orderApplicationService.createOrder(command, TriggerContext.manual());

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
            items1.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-500", 1, new BigDecimal("10.00")));
            CreateOrderCommand command1 = new CreateOrderCommand("ORDER-005", items1);

            List<CreateOrderCommand.OrderLineItemDto> items2 = new ArrayList<>();
            items2.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-501", 2, new BigDecimal("20.00")));
            CreateOrderCommand command2 = new CreateOrderCommand("ORDER-006", items2);

            Order order1 = orderApplicationService.createOrder(command1, TriggerContext.manual());
            Order order2 = orderApplicationService.createOrder(command2, TriggerContext.manual());

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
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-300", 1, new BigDecimal("99.99")));
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-301", 2, new BigDecimal("0.01")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-007", items);

            orderApplicationService.createOrder(command, TriggerContext.manual());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-007");
            assertTrue(persistedOrder.isPresent());

            Order order = persistedOrder.get();
            assertEquals(new BigDecimal("99.99"), order.getOrderLineItems().get(0).getPrice());
            assertEquals(new BigDecimal("0.01"), order.getOrderLineItems().get(1).getPrice());
        }

        @Test
        void shouldThrowExceptionWhenCreatingOrderWithDuplicateId() {
            List<CreateOrderCommand.OrderLineItemDto> items1 = new ArrayList<>();
            items1.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-800", 5, new BigDecimal("50.00")));
            CreateOrderCommand command1 = new CreateOrderCommand("ORDER-DUPLICATE", items1);

            orderApplicationService.createOrder(command1, TriggerContext.manual());

            List<CreateOrderCommand.OrderLineItemDto> items2 = new ArrayList<>();
            items2.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-801", 3, new BigDecimal("30.00")));
            CreateOrderCommand command2 = new CreateOrderCommand("ORDER-DUPLICATE", items2);

            com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException exception =
                    assertThrows(
                            com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException
                                    .class,
                            () -> {
                                orderApplicationService.createOrder(
                                        command2, TriggerContext.manual());
                            });

            assertTrue(exception.getMessage().contains("ORDER-DUPLICATE"));
            assertTrue(exception.getMessage().contains("already exists"));
        }

        @Test
        void shouldCreateOrderWithFutureScheduledPickupTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-900", 5, new BigDecimal("50.00")));

            LocalDateTime futurePickupTime = LocalDateTime.now().plusHours(3);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-SCHEDULED-001", items, futurePickupTime, null);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals(OrderStatus.SCHEDULED, createdOrder.getStatus());
            assertNotNull(createdOrder.getScheduledPickupTime());
            assertNotNull(createdOrder.getFulfillmentLeadTime());
            assertEquals(futurePickupTime, createdOrder.getScheduledPickupTime().getPickupTime());
            assertEquals(120, createdOrder.getFulfillmentLeadTime().getMinutes());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-SCHEDULED-001");
            assertTrue(persistedOrder.isPresent());
            assertEquals(OrderStatus.SCHEDULED, persistedOrder.get().getStatus());
        }

        @Test
        void shouldCreateOrderWithPastScheduledPickupTimeAsAwaitingFulfillment() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-901", 10, new BigDecimal("100.00")));

            LocalDateTime pastPickupTime = LocalDateTime.now().minusHours(1);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-IMMEDIATE-001", items, pastPickupTime, null);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, createdOrder.getStatus());
            assertNotNull(createdOrder.getScheduledPickupTime());
            assertNotNull(createdOrder.getFulfillmentLeadTime());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-IMMEDIATE-001");
            assertTrue(persistedOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, persistedOrder.get().getStatus());
        }

        @Test
        void shouldCreateOrderWithCustomFulfillmentLeadTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-902", 3, new BigDecimal("30.00")));

            LocalDateTime futurePickupTime = LocalDateTime.now().plusHours(5);
            Duration customLeadTime = Duration.ofHours(3);
            CreateOrderCommand command =
                    new CreateOrderCommand(
                            "ORDER-CUSTOM-LEAD-001", items, futurePickupTime, customLeadTime);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals(OrderStatus.SCHEDULED, createdOrder.getStatus());
            assertEquals(180, createdOrder.getFulfillmentLeadTime().getMinutes());
            assertEquals(3, createdOrder.getFulfillmentLeadTime().getHours());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-CUSTOM-LEAD-001");
            assertTrue(persistedOrder.isPresent());
            assertEquals(180, persistedOrder.get().getFulfillmentLeadTime().getMinutes());
        }

        @Test
        void shouldCreateOrderWithDefaultFulfillmentLeadTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-903", 7, new BigDecimal("70.00")));

            LocalDateTime futurePickupTime = LocalDateTime.now().plusHours(4);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-DEFAULT-LEAD-001", items, futurePickupTime, null);

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals(OrderStatus.SCHEDULED, createdOrder.getStatus());
            assertEquals(120, createdOrder.getFulfillmentLeadTime().getMinutes());
            assertEquals(2, createdOrder.getFulfillmentLeadTime().getHours());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-DEFAULT-LEAD-001");
            assertTrue(persistedOrder.isPresent());
            assertEquals(120, persistedOrder.get().getFulfillmentLeadTime().getMinutes());
        }

        @Test
        void shouldCreateOrderWithSchedulingFieldsPersisted() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-904", 2, new BigDecimal("20.00")));

            LocalDateTime scheduledTime = LocalDateTime.of(2025, 11, 6, 14, 0);
            Duration leadTime = Duration.ofMinutes(90);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-PERSIST-001", items, scheduledTime, leadTime);

            orderApplicationService.createOrder(command, TriggerContext.manual());

            Optional<Order> persistedOrder = orderRepository.findById("ORDER-PERSIST-001");
            assertTrue(persistedOrder.isPresent());

            Order order = persistedOrder.get();
            assertNotNull(order.getScheduledPickupTime());
            assertNotNull(order.getFulfillmentLeadTime());
            assertEquals(scheduledTime, order.getScheduledPickupTime().getPickupTime());
            assertEquals(90, order.getFulfillmentLeadTime().getMinutes());
        }
    }

    @Nested
    class initiateFulfillment {

        @Test
        void shouldInitiateFulfillmentForScheduledOrder() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-1000", 10, new BigDecimal("100.00")));

            LocalDateTime futurePickupTime = LocalDateTime.now().plusHours(2);
            CreateOrderCommand createCommand =
                    new CreateOrderCommand("ORDER-INITIATE-001", items, futurePickupTime, null);

            orderApplicationService.createOrder(createCommand, TriggerContext.manual());

            Optional<Order> scheduledOrder = orderRepository.findById("ORDER-INITIATE-001");
            assertTrue(scheduledOrder.isPresent());
            assertEquals(OrderStatus.SCHEDULED, scheduledOrder.get().getStatus());

            InitiateFulfillmentCommand initiateCommand =
                    new InitiateFulfillmentCommand("ORDER-INITIATE-001");
            orderApplicationService.initiateFulfillment(initiateCommand, TriggerContext.manual());

            Optional<Order> awaitingOrder = orderRepository.findById("ORDER-INITIATE-001");
            assertTrue(awaitingOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, awaitingOrder.get().getStatus());
        }

        @Test
        void shouldInitiateFulfillmentForCreatedOrder() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-1001", 5, new BigDecimal("50.00")));

            CreateOrderCommand createCommand = new CreateOrderCommand("ORDER-INITIATE-002", items);
            orderApplicationService.createOrder(createCommand, TriggerContext.manual());

            Optional<Order> createdOrder = orderRepository.findById("ORDER-INITIATE-002");
            assertTrue(createdOrder.isPresent());
            assertEquals(OrderStatus.CREATED, createdOrder.get().getStatus());

            InitiateFulfillmentCommand initiateCommand =
                    new InitiateFulfillmentCommand("ORDER-INITIATE-002");
            orderApplicationService.initiateFulfillment(initiateCommand, TriggerContext.manual());

            Optional<Order> awaitingOrder = orderRepository.findById("ORDER-INITIATE-002");
            assertTrue(awaitingOrder.isPresent());
            assertEquals(OrderStatus.AWAITING_FULFILLMENT, awaitingOrder.get().getStatus());
        }

        @Test
        void shouldThrowExceptionWhenInitiatingFulfillmentForNonExistentOrder() {
            InitiateFulfillmentCommand command =
                    new InitiateFulfillmentCommand("NON-EXISTENT-ORDER");

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                orderApplicationService.initiateFulfillment(
                                        command, TriggerContext.manual());
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains("NON-EXISTENT-ORDER"));
        }
    }
}
