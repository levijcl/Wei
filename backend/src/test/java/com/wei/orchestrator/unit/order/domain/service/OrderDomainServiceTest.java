package com.wei.orchestrator.unit.order.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.order.domain.exception.OrderAlreadyExistsException;
import com.wei.orchestrator.order.domain.model.Order;
import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import com.wei.orchestrator.order.domain.repository.OrderRepository;
import com.wei.orchestrator.order.domain.service.OrderDomainService;
import java.time.Duration;
import java.time.LocalDateTime;
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

    @Nested
    class processOrderSchedulingMethodTest {

        @Test
        void shouldScheduleForLaterWhenPickupTimeIsInFuture() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime futurePickupTime = currentTime.plusHours(5);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(futurePickupTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(2));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, currentTime);

            verify(order, times(1))
                    .scheduleForLaterFulfillment(scheduledPickupTime, fulfillmentLeadTime);
            verify(order, never()).setScheduledPickupTime(any());
            verify(order, never()).setFulfillmentLeadTime(any());
            verify(order, never()).markReadyForFulfillment();
        }

        @Test
        void shouldMarkReadyForFulfillmentWhenPickupTimeIsInPast() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime pastPickupTime = currentTime.minusHours(1);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pastPickupTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(2));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, currentTime);

            verify(order, times(1)).setScheduledPickupTime(scheduledPickupTime);
            verify(order, times(1)).setFulfillmentLeadTime(fulfillmentLeadTime);
            verify(order, times(1)).markReadyForFulfillment();
            verify(order, never()).scheduleForLaterFulfillment(any(), any());
        }

        @Test
        void shouldMarkReadyForFulfillmentWhenPickupTimeIsNow() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(currentTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(2));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, currentTime);

            verify(order, times(1)).setScheduledPickupTime(scheduledPickupTime);
            verify(order, times(1)).setFulfillmentLeadTime(fulfillmentLeadTime);
            verify(order, times(1)).markReadyForFulfillment();
            verify(order, never()).scheduleForLaterFulfillment(any(), any());
        }

        @Test
        void shouldHandleDifferentLeadTimes() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime futurePickupTime = currentTime.plusHours(10);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(futurePickupTime);
            FulfillmentLeadTime shortLeadTime = new FulfillmentLeadTime(Duration.ofMinutes(30));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, shortLeadTime, currentTime);

            verify(order, times(1)).scheduleForLaterFulfillment(scheduledPickupTime, shortLeadTime);
        }

        @Test
        void shouldHandleVeryFuturePickupTime() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime veryFuturePickupTime = currentTime.plusDays(7);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(veryFuturePickupTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(4));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, currentTime);

            verify(order, times(1))
                    .scheduleForLaterFulfillment(scheduledPickupTime, fulfillmentLeadTime);
            verify(order, never()).markReadyForFulfillment();
        }

        @Test
        void shouldHandleVeryPastPickupTime() {
            Order order = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime veryPastPickupTime = currentTime.minusDays(2);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(veryPastPickupTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(2));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, currentTime);

            verify(order, times(1)).setScheduledPickupTime(scheduledPickupTime);
            verify(order, times(1)).setFulfillmentLeadTime(fulfillmentLeadTime);
            verify(order, times(1)).markReadyForFulfillment();
            verify(order, never()).scheduleForLaterFulfillment(any(), any());
        }

        @Test
        void shouldProcessMultipleDifferentOrdersIndependently() {
            Order order1 = mock(Order.class);
            Order order2 = mock(Order.class);
            LocalDateTime currentTime = LocalDateTime.now();

            LocalDateTime futurePickupTime = currentTime.plusHours(5);
            ScheduledPickupTime futureScheduledPickupTime =
                    new ScheduledPickupTime(futurePickupTime);
            FulfillmentLeadTime leadTime1 = new FulfillmentLeadTime(Duration.ofHours(2));

            LocalDateTime pastPickupTime = currentTime.minusHours(1);
            ScheduledPickupTime pastScheduledPickupTime = new ScheduledPickupTime(pastPickupTime);
            FulfillmentLeadTime leadTime2 = new FulfillmentLeadTime(Duration.ofHours(3));

            orderDomainService.processOrderScheduling(
                    order1, futureScheduledPickupTime, leadTime1, currentTime);
            orderDomainService.processOrderScheduling(
                    order2, pastScheduledPickupTime, leadTime2, currentTime);

            verify(order1, times(1))
                    .scheduleForLaterFulfillment(futureScheduledPickupTime, leadTime1);
            verify(order1, never()).markReadyForFulfillment();

            verify(order2, times(1)).setScheduledPickupTime(pastScheduledPickupTime);
            verify(order2, times(1)).setFulfillmentLeadTime(leadTime2);
            verify(order2, times(1)).markReadyForFulfillment();
            verify(order2, never()).scheduleForLaterFulfillment(any(), any());
        }

        @Test
        void shouldUseProvidedCurrentTimeForComparison() {
            Order order = mock(Order.class);
            LocalDateTime customCurrentTime = LocalDateTime.of(2025, 11, 6, 10, 0);
            LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 9, 0);
            ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
            FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(Duration.ofHours(2));

            orderDomainService.processOrderScheduling(
                    order, scheduledPickupTime, fulfillmentLeadTime, customCurrentTime);

            verify(order, times(1)).setScheduledPickupTime(scheduledPickupTime);
            verify(order, times(1)).setFulfillmentLeadTime(fulfillmentLeadTime);
            verify(order, times(1)).markReadyForFulfillment();
            verify(order, never()).scheduleForLaterFulfillment(any(), any());
        }
    }
}
