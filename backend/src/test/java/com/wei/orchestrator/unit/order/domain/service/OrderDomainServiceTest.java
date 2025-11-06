package com.wei.orchestrator.unit.order.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.domain.service.OrderDomainService;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderDomainServiceTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks private OrderDomainService orderDomainService;

    @Nested
    class validateOrderCreationMethodTest {
        @Test
        void shouldPassValidationWhenOrderIdDoesNotExist() {
            String orderId = "ORDER-001";
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> orderDomainService.validateOrderCreation(orderId));

            verify(orderRepository, times(1)).findById(orderId);
        }

        @Test
        void shouldThrowExceptionWhenOrderIdAlreadyExists() {
            String orderId = "ORDER-001";
            Order existingOrder = mock(Order.class);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            OrderAlreadyExistsException exception =
                    assertThrows(
                            OrderAlreadyExistsException.class,
                            () -> {
                                orderDomainService.validateOrderCreation(orderId);
                            });

            assertTrue(exception.getMessage().contains("ORDER-001"));
            assertTrue(exception.getMessage().contains("already exists"));
            verify(orderRepository, times(1)).findById(orderId);
        }

        @Test
        void shouldThrowExceptionWhenOrderIdIsNull() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                orderDomainService.validateOrderCreation(null);
                            });

            assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
            verify(orderRepository, never()).findById(any());
        }

        @Test
        void shouldThrowExceptionWhenOrderIdIsEmpty() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                orderDomainService.validateOrderCreation("");
                            });

            assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
            verify(orderRepository, never()).findById(any());
        }

        @Test
        void shouldThrowExceptionWhenOrderIdIsBlank() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                orderDomainService.validateOrderCreation("   ");
                            });

            assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
            verify(orderRepository, never()).findById(any());
        }
    }
}
