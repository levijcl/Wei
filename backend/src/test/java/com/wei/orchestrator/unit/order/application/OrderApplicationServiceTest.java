package com.wei.orchestrator.unit.order.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.InitiateFulfillmentCommand;
import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.OrderStatus;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.domain.service.OrderDomainService;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock private OrderRepository orderRepository;

    @Mock private OrderDomainService orderDomainService;

    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private OrderApplicationService orderApplicationService;

    @Nested
    class createOrderTest {

        @Test
        void shouldCreateOrderSuccessfully() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-002", 5, new BigDecimal("50.00")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals("ORDER-001", createdOrder.getOrderId());
            assertEquals(OrderStatus.CREATED, createdOrder.getStatus());
            assertEquals(2, createdOrder.getOrderLineItems().size());
            assertEquals("SKU-001", createdOrder.getOrderLineItems().get(0).getSku());
            assertEquals(10, createdOrder.getOrderLineItems().get(0).getQuantity());

            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        void shouldThrowExceptionWhenCreatingCommandWithEmptyItems() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                new CreateOrderCommand("ORDER-002", new ArrayList<>());
                            });

            assertTrue(exception.getMessage().contains("Order must have at least one item"));
        }

        @Test
        void shouldConvertCommandDtoToDomainModel() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-100", 3, new BigDecimal("30.50")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-003", items);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertEquals("SKU-100", createdOrder.getOrderLineItems().get(0).getSku());
            assertEquals(3, createdOrder.getOrderLineItems().get(0).getQuantity());
            assertEquals(
                    new BigDecimal("30.50"), createdOrder.getOrderLineItems().get(0).getPrice());
        }

        @Test
        void shouldCallRepositorySaveExactlyOnce() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto("SKU-100", 3, new BigDecimal("30.50")));
            CreateOrderCommand command = new CreateOrderCommand("ORDER-004", items);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderApplicationService.createOrder(command, TriggerContext.manual());

            verify(orderRepository, times(1)).save(any(Order.class));
            verifyNoMoreInteractions(orderRepository);
        }

        @Test
        void shouldThrowExceptionWhenOrderIdAlreadyExists() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));
            CreateOrderCommand command = new CreateOrderCommand("ORDER-005", items);

            doThrow(new OrderAlreadyExistsException("Order with ID ORDER-005 already exists"))
                    .when(orderDomainService)
                    .validateOrderCreation("ORDER-005");

            OrderAlreadyExistsException exception =
                    assertThrows(
                            OrderAlreadyExistsException.class,
                            () -> {
                                orderApplicationService.createOrder(
                                        command, TriggerContext.manual());
                            });

            assertTrue(exception.getMessage().contains("ORDER-005"));
            assertTrue(exception.getMessage().contains("already exists"));
            verify(orderDomainService, times(1)).validateOrderCreation("ORDER-005");
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void shouldCreateOrderWithScheduledPickupTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));

            LocalDateTime scheduledPickupTime = LocalDateTime.now().plusHours(3);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-006", items, scheduledPickupTime, null);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            assertEquals("ORDER-006", createdOrder.getOrderId());
            verify(orderDomainService, times(1))
                    .processOrderScheduling(
                            any(Order.class),
                            any(ScheduledPickupTime.class),
                            any(FulfillmentLeadTime.class),
                            any(LocalDateTime.class));
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        void shouldCreateOrderWithCustomFulfillmentLeadTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));

            LocalDateTime scheduledPickupTime = LocalDateTime.now().plusHours(5);
            Duration customLeadTime = Duration.ofHours(3);
            CreateOrderCommand command =
                    new CreateOrderCommand("ORDER-007", items, scheduledPickupTime, customLeadTime);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            Order createdOrder =
                    orderApplicationService.createOrder(command, TriggerContext.manual());

            assertNotNull(createdOrder);
            verify(orderDomainService, times(1))
                    .processOrderScheduling(
                            any(Order.class),
                            any(ScheduledPickupTime.class),
                            any(FulfillmentLeadTime.class),
                            any(LocalDateTime.class));
        }

        @Test
        void shouldNotCallProcessOrderSchedulingWhenNoScheduledPickupTime() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-008", items);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderApplicationService.createOrder(command, TriggerContext.manual());

            verify(orderDomainService, never())
                    .processOrderScheduling(
                            any(Order.class),
                            any(ScheduledPickupTime.class),
                            any(FulfillmentLeadTime.class),
                            any(LocalDateTime.class));
        }

        @Test
        void shouldPublishDomainEventsAfterSave() {
            List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
            items.add(
                    new CreateOrderCommand.OrderLineItemDto(
                            "SKU-001", 10, new BigDecimal("100.00")));

            CreateOrderCommand command = new CreateOrderCommand("ORDER-009", items);

            when(orderRepository.save(any(Order.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderApplicationService.createOrder(command, TriggerContext.manual());

            verify(orderRepository, times(1)).save(any(Order.class));
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
    }

    @Nested
    class initiateFulfillmentTest {

        @Test
        void shouldInitiateFulfillmentSuccessfully() {
            String orderId = "ORDER-101";
            InitiateFulfillmentCommand command = new InitiateFulfillmentCommand(orderId);

            Order order = new Order(orderId, List.of(new OrderLineItem("SKU1", 1, BigDecimal.TEN)));
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderApplicationService.initiateFulfillment(command);

            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, times(1)).save(order);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        void shouldThrowExceptionWhenOrderNotFound() {
            String orderId = "NON-EXISTENT-ORDER";
            InitiateFulfillmentCommand command = new InitiateFulfillmentCommand(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                orderApplicationService.initiateFulfillment(command);
                            });

            assertTrue(exception.getMessage().contains("Order not found"));
            assertTrue(exception.getMessage().contains(orderId));
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        void shouldPublishDomainEventsAfterInitiateFulfillment() {
            String orderId = "ORDER-102";
            InitiateFulfillmentCommand command = new InitiateFulfillmentCommand(orderId);

            Order order = mock(Order.class);
            when(order.getDomainEvents()).thenReturn(new ArrayList<>());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderApplicationService.initiateFulfillment(command);

            verify(order, times(1)).getDomainEvents();
            verify(order, times(1)).clearDomainEvents();
        }

        @Test
        void shouldSaveOrderAfterMarkingReadyForFulfillment() {
            String orderId = "ORDER-103";
            InitiateFulfillmentCommand command = new InitiateFulfillmentCommand(orderId);

            Order order = mock(Order.class);
            when(order.getDomainEvents()).thenReturn(new ArrayList<>());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(any(Order.class))).thenReturn(order);

            orderApplicationService.initiateFulfillment(command);

            verify(order, times(1)).markReadyForFulfillment();
            verify(orderRepository, times(1)).save(order);
        }
    }
}
