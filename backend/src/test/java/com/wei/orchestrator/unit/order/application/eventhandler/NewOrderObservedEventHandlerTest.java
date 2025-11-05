package com.wei.orchestrator.unit.order.application.eventhandler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.OrderApplicationService;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.eventhandler.NewOrderObservedEventHandler;
import com.wei.orchestrator.order.application.translator.ObservationToOrderTranslator;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.OrderLineItem;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewOrderObservedEventHandlerTest {

    @Mock private OrderApplicationService orderApplicationService;

    @Mock private OrderRepository orderRepository;
    @Mock private ObservationToOrderTranslator observationToOrderTranslator;

    @InjectMocks private NewOrderObservedEventHandler eventHandler;

    @Nested
    class handleNewOrderObservedTest {

        @Test
        void shouldCreateOrderWhenEventReceivedAndOrderDoesNotExist() {
            String orderId = "ORDER-001";
            NewOrderObservedEvent event = createTestEvent(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
            when(observationToOrderTranslator.translate(any(ObservationResult.class)))
                    .thenReturn(new CreateOrderCommand());
            when(orderApplicationService.createOrder(any(CreateOrderCommand.class)))
                    .thenReturn(createMockOrder(orderId));

            eventHandler.handleNewOrderObserved(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(observationToOrderTranslator).translate(any(ObservationResult.class));
            verify(orderApplicationService, times(1)).createOrder(any(CreateOrderCommand.class));
        }

        @Test
        void shouldSkipOrderCreationWhenOrderAlreadyExists() {
            String orderId = "ORDER-002";
            NewOrderObservedEvent event = createTestEvent(orderId);
            Order existingOrder = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            eventHandler.handleNewOrderObserved(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(observationToOrderTranslator, never()).translate(any(ObservationResult.class));
            verify(orderApplicationService, never()).createOrder(any(CreateOrderCommand.class));
        }

        @Test
        void shouldThrowExceptionWhenOrderCreationFails() {
            String orderId = "ORDER-004";
            NewOrderObservedEvent event = createTestEvent(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
            when(observationToOrderTranslator.translate(event.getObservedOrder()))
                    .thenReturn(new CreateOrderCommand());
            when(orderApplicationService.createOrder(any(CreateOrderCommand.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            RuntimeException exception =
                    assertThrows(
                            RuntimeException.class,
                            () -> {
                                eventHandler.handleNewOrderObserved(event);
                            });

            assertTrue(exception.getMessage().contains("Database connection failed"));
            verify(orderRepository, times(1)).findById(orderId);
            verify(orderApplicationService, times(1)).createOrder(any(CreateOrderCommand.class));
        }

        @Test
        void shouldVerifyIdempotencyCheckHappensBeforeOrderCreation() {
            String orderId = "ORDER-006";
            NewOrderObservedEvent event = createTestEvent(orderId);
            Order existingOrder = createMockOrder(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            eventHandler.handleNewOrderObserved(event);

            verify(orderRepository, times(1)).findById(orderId);
            verify(orderApplicationService, never()).createOrder(any(CreateOrderCommand.class));
            verifyNoMoreInteractions(orderApplicationService);
        }
    }

    private NewOrderObservedEvent createTestEvent(String orderId) {
        List<ObservedOrderItem> items =
                Arrays.asList(
                        new ObservedOrderItem("SKU-001", "Product 1", 10, new BigDecimal("100.00")),
                        new ObservedOrderItem("SKU-002", "Product 2", 5, new BigDecimal("50.00")));

        ObservationResult observationResult =
                new ObservationResult(
                        orderId,
                        "John Doe",
                        "john@example.com",
                        "123 Main St",
                        "STANDARD",
                        "WH-001",
                        "NEW",
                        items,
                        LocalDateTime.now());

        return new NewOrderObservedEvent("observer-1", observationResult);
    }

    private Order createMockOrder(String orderId) {
        List<OrderLineItem> lineItems =
                Arrays.asList(
                        new OrderLineItem("SKU-001", 10, new BigDecimal("100.00")),
                        new OrderLineItem("SKU-002", 5, new BigDecimal("50.00")));

        return new Order(orderId, lineItems);
    }
}
