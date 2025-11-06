package com.wei.orchestrator.unit.observation.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.domain.event.NewOrderObservedEvent;
import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderObserverTest {

    @Mock private OrderSourcePort orderSourcePort;

    @Test
    void shouldCreateOrderObserverWithValidParameters() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);

        assertNotNull(orderObserver);
        assertEquals("observer-1", orderObserver.getObserverId());
        assertTrue(orderObserver.isActive());
        assertNull(orderObserver.getLastPolledTimestamp());
        assertTrue(orderObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsNull() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new OrderObserver(null, sourceEndpoint, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsEmpty() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new OrderObserver("", sourceEndpoint, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenSourceEndpointIsNull() {
        PollingInterval pollingInterval = new PollingInterval(60);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new OrderObserver("observer-1", null, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Source endpoint cannot be null"));
    }

    @Test
    void shouldThrowExceptionWhenPollingIntervalIsNull() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new OrderObserver("observer-1", sourceEndpoint, null);
                        });

        assertTrue(exception.getMessage().contains("Polling interval cannot be null"));
    }

    @Test
    void shouldReturnTrueWhenNeverPolledBefore() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);

        boolean shouldPoll = orderObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenPolledRecently() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        orderObserver.setLastPolledTimestamp(LocalDateTime.now());

        boolean shouldPoll = orderObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldReturnTrueWhenPollingIntervalHasPassed() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        orderObserver.setLastPolledTimestamp(LocalDateTime.now().minusSeconds(61));

        boolean shouldPoll = orderObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenObserverIsInactive() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        orderObserver.deactivate();

        boolean shouldPoll = orderObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldNotPollWhenShouldPollReturnsFalse() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        orderObserver.setLastPolledTimestamp(LocalDateTime.now());

        orderObserver.pollOrderSource(orderSourcePort);

        verify(orderSourcePort, never()).fetchNewOrders(any(), any());
        assertTrue(orderObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldFetchOrdersWhenShouldPollReturnsTrue() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        List<ObservationResult> mockResults = createMockObservationResults(2);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

        orderObserver.pollOrderSource(orderSourcePort);

        verify(orderSourcePort).fetchNewOrders(sourceEndpoint, null);
        assertNotNull(orderObserver.getLastPolledTimestamp());
    }

    @Test
    void shouldCollectDomainEventsWhenOrdersAreFound() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        List<ObservationResult> mockResults = createMockObservationResults(3);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

        orderObserver.pollOrderSource(orderSourcePort);

        List<Object> domainEvents = orderObserver.getDomainEvents();
        assertEquals(3, domainEvents.size());

        for (Object event : domainEvents) {
            assertInstanceOf(NewOrderObservedEvent.class, event);
            NewOrderObservedEvent orderEvent = (NewOrderObservedEvent) event;
            assertEquals("observer-1", orderEvent.getObserverId());
            assertNotNull(orderEvent.getObservedOrder());
        }
    }

    @Test
    void shouldNotCollectEventsWhenNoOrdersFound() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(Collections.emptyList());

        orderObserver.pollOrderSource(orderSourcePort);

        assertTrue(orderObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldUpdateLastPolledTimestampAfterSuccessfulPoll() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        LocalDateTime beforePoll = LocalDateTime.now();
        List<ObservationResult> mockResults = createMockObservationResults(1);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

        orderObserver.pollOrderSource(orderSourcePort);

        assertNotNull(orderObserver.getLastPolledTimestamp());
        assertTrue(
                orderObserver.getLastPolledTimestamp().isAfter(beforePoll)
                        || orderObserver.getLastPolledTimestamp().isEqual(beforePoll));
    }

    @Test
    void shouldClearAllDomainEvents() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        List<ObservationResult> mockResults = createMockObservationResults(2);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);
        orderObserver.pollOrderSource(orderSourcePort);

        orderObserver.clearDomainEvents();

        assertTrue(orderObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableListOfDomainEvents() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        List<ObservationResult> mockResults = createMockObservationResults(1);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);
        orderObserver.pollOrderSource(orderSourcePort);

        List<Object> events = orderObserver.getDomainEvents();

        assertThrows(UnsupportedOperationException.class, () -> events.add(new Object()));
    }

    @Test
    void shouldActivateObserver() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        orderObserver.deactivate();

        orderObserver.activate();

        assertTrue(orderObserver.isActive());
    }

    @Test
    void shouldDeactivateObserver() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);

        orderObserver.deactivate();

        assertFalse(orderObserver.isActive());
    }

    @Test
    void shouldPassLastPolledTimestampWhenPollingAgain() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);
        OrderObserver orderObserver =
                new OrderObserver("observer-1", sourceEndpoint, pollingInterval);
        LocalDateTime firstPollTime = LocalDateTime.now().minusMinutes(5);
        orderObserver.setLastPolledTimestamp(firstPollTime);
        List<ObservationResult> mockResults = createMockObservationResults(1);
        when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

        orderObserver.pollOrderSource(orderSourcePort);

        verify(orderSourcePort).fetchNewOrders(eq(sourceEndpoint), eq(firstPollTime));
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
