package com.wei.orchestrator.unit.observation.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import com.wei.orchestrator.observation.application.command.CreateOrderObserverCommand;
import com.wei.orchestrator.observation.application.command.PollOrderSourceCommand;
import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import com.wei.orchestrator.observation.domain.repository.OrderObserverRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderObserverApplicationServiceTest {

    @Mock private OrderObserverRepository orderObserverRepository;

    @Mock private OrderSourcePort orderSourcePort;

    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private OrderObserverApplicationService orderObserverApplicationService;

    @Nested
    class createOrderObserverTest {

        @Test
        void shouldCreateOrderObserverSuccessfully() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "testuser",
                            "testpass",
                            60);

            when(orderObserverRepository.save(any(OrderObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String observerId = orderObserverApplicationService.createOrderObserver(command);

            assertEquals("observer-1", observerId);
            verify(orderObserverRepository, times(1)).save(any(OrderObserver.class));
        }

        @Test
        void shouldCallRepositorySaveExactlyOnce() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-2",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            120);

            when(orderObserverRepository.save(any(OrderObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderObserverApplicationService.createOrderObserver(command);

            verify(orderObserverRepository, times(1)).save(any(OrderObserver.class));
            verifyNoMoreInteractions(orderObserverRepository);
        }

        @Test
        void shouldCreateObserverWithCorrectSourceEndpoint() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-3",
                            "jdbc:oracle:thin:@db.example.com:1521:PROD",
                            "produser",
                            "prodpass",
                            90);

            ArgumentCaptor<OrderObserver> captor = ArgumentCaptor.forClass(OrderObserver.class);
            when(orderObserverRepository.save(any(OrderObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            orderObserverApplicationService.createOrderObserver(command);

            verify(orderObserverRepository).save(captor.capture());
            OrderObserver savedObserver = captor.getValue();
            assertEquals("observer-3", savedObserver.getObserverId());
            assertEquals(
                    "jdbc:oracle:thin:@db.example.com:1521:PROD",
                    savedObserver.getSourceEndpoint().getJdbcUrl());
            assertEquals("produser", savedObserver.getSourceEndpoint().getUsername());
        }
    }

    @Nested
    class pollOrderSourceTest {

        @Test
        void shouldPollOrderSourceSuccessfully() {
            OrderObserver mockObserver = createMockOrderObserver("observer-1");
            PollOrderSourceCommand command = new PollOrderSourceCommand("observer-1");

            when(orderObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));
            when(orderSourcePort.fetchNewOrders(any(), any()))
                    .thenReturn(createMockObservationResults(2));

            orderObserverApplicationService.pollOrderSource(command, TriggerContext.manual());

            verify(orderObserverRepository).findById("observer-1");
            verify(orderObserverRepository).save(mockObserver);
            verify(eventPublisher, times(2)).publishEvent(any(NewOrderObservedEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFound() {
            PollOrderSourceCommand command = new PollOrderSourceCommand("non-existent");

            when(orderObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    orderObserverApplicationService.pollOrderSource(
                                            command, TriggerContext.manual()));

            assertTrue(exception.getMessage().contains("OrderObserver not found"));
            verify(orderObserverRepository).findById("non-existent");
            verify(orderObserverRepository, never()).save(any());
        }

        @Test
        void shouldPublishEventsAfterSuccessfulSave() {
            OrderObserver mockObserver = createMockOrderObserver("observer-2");
            PollOrderSourceCommand command = new PollOrderSourceCommand("observer-2");

            when(orderObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));
            when(orderSourcePort.fetchNewOrders(any(), any()))
                    .thenReturn(createMockObservationResults(1));

            orderObserverApplicationService.pollOrderSource(command, TriggerContext.manual());

            verify(orderObserverRepository).save(mockObserver);
            verify(eventPublisher, times(1)).publishEvent(any(NewOrderObservedEvent.class));
        }

        @Test
        void shouldNotPublishEventsWhenNoOrdersFound() {
            OrderObserver mockObserver = createMockOrderObserver("observer-3");
            PollOrderSourceCommand command = new PollOrderSourceCommand("observer-3");

            when(orderObserverRepository.findById("observer-3"))
                    .thenReturn(Optional.of(mockObserver));
            when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(new ArrayList<>());

            orderObserverApplicationService.pollOrderSource(command, TriggerContext.manual());

            verify(orderObserverRepository).save(mockObserver);
            verify(eventPublisher, never()).publishEvent(any(NewOrderObservedEvent.class));
        }

        @Test
        void shouldClearDomainEventsAfterPublishing() {
            OrderObserver mockObserver = createMockOrderObserver("observer-4");
            PollOrderSourceCommand command = new PollOrderSourceCommand("observer-4");

            when(orderObserverRepository.findById("observer-4"))
                    .thenReturn(Optional.of(mockObserver));
            when(orderSourcePort.fetchNewOrders(any(), any()))
                    .thenReturn(createMockObservationResults(3));

            orderObserverApplicationService.pollOrderSource(command, TriggerContext.manual());

            assertTrue(mockObserver.getDomainEvents().isEmpty());
        }
    }

    @Nested
    class pollAllActiveObserversTest {

        @Test
        void shouldPollAllActiveObservers() {
            OrderObserver observer1 = createMockOrderObserver("observer-1");
            OrderObserver observer2 = createMockOrderObserver("observer-2");
            List<OrderObserver> activeObservers = Arrays.asList(observer1, observer2);

            when(orderObserverRepository.findAllActive()).thenReturn(activeObservers);
            when(orderObserverRepository.findById("observer-1")).thenReturn(Optional.of(observer1));
            when(orderObserverRepository.findById("observer-2")).thenReturn(Optional.of(observer2));
            when(orderSourcePort.fetchNewOrders(any(), any()))
                    .thenReturn(createMockObservationResults(1));

            orderObserverApplicationService.pollAllActiveObservers();

            verify(orderObserverRepository).findAllActive();
            verify(orderObserverRepository, times(2)).save(any(OrderObserver.class));
        }

        @Test
        void shouldHandleEmptyActiveObserversList() {
            when(orderObserverRepository.findAllActive()).thenReturn(new ArrayList<>());

            orderObserverApplicationService.pollAllActiveObservers();

            verify(orderObserverRepository).findAllActive();
            verify(orderSourcePort, never()).fetchNewOrders(any(), any());
        }
    }

    @Nested
    class activateObserverTest {

        @Test
        void shouldActivateObserver() {
            OrderObserver mockObserver = createMockOrderObserver("observer-1");
            mockObserver.deactivate();

            when(orderObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            orderObserverApplicationService.activateObserver("observer-1");

            assertTrue(mockObserver.isActive());
            verify(orderObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForActivation() {
            when(orderObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> orderObserverApplicationService.activateObserver("non-existent"));

            assertTrue(exception.getMessage().contains("OrderObserver not found"));
        }
    }

    @Nested
    class deactivateObserverTest {

        @Test
        void shouldDeactivateObserver() {
            OrderObserver mockObserver = createMockOrderObserver("observer-1");

            when(orderObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            orderObserverApplicationService.deactivateObserver("observer-1");

            assertFalse(mockObserver.isActive());
            verify(orderObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForDeactivation() {
            when(orderObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                    orderObserverApplicationService.deactivateObserver(
                                            "non-existent"));

            assertTrue(exception.getMessage().contains("OrderObserver not found"));
        }
    }

    private OrderObserver createMockOrderObserver(String observerId) {
        return new OrderObserver(
                observerId,
                new com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint(
                        "jdbc:oracle:thin:@localhost:1521:XE", "user", "pass"),
                new com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval(60));
    }

    private List<ObservationResult> createMockObservationResults(int count) {
        List<ObservationResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-" + i, "Product " + i, 1, BigDecimal.valueOf(100.0)));

            ObservationResult result =
                    new ObservationResult(
                            "order-" + i,
                            "Customer " + i,
                            "customer" + i + "@example.com",
                            "Address " + i,
                            "TYPE_A",
                            "WH001",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());
            results.add(result);
        }
        return results;
    }
}
