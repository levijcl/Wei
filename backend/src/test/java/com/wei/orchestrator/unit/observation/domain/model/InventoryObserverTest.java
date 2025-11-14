package com.wei.orchestrator.unit.observation.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.infrastructure.adapter.dto.InventorySnapshotDto;
import com.wei.orchestrator.observation.domain.event.InventorySnapshotObservedEvent;
import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationRule;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryObserverTest {

    @Mock private InventoryPort inventoryPort;

    @Test
    void shouldCreateInventoryObserverWithValidParameters() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);

        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        assertNotNull(inventoryObserver);
        assertEquals("observer-1", inventoryObserver.getObserverId());
        assertTrue(inventoryObserver.isActive());
        assertNull(inventoryObserver.getLastPolledTimestamp());
        assertTrue(inventoryObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsNull() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new InventoryObserver(null, observationRule, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenObserverIdIsEmpty() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new InventoryObserver("", observationRule, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observer ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenObservationRuleIsNull() {
        PollingInterval pollingInterval = new PollingInterval(3600);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new InventoryObserver("observer-1", null, pollingInterval);
                        });

        assertTrue(exception.getMessage().contains("Observation rule cannot be null"));
    }

    @Test
    void shouldThrowExceptionWhenPollingIntervalIsNull() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new InventoryObserver("observer-1", observationRule, null);
                        });

        assertTrue(exception.getMessage().contains("Polling interval cannot be null"));
    }

    @Test
    void shouldReturnTrueWhenNeverPolledBefore() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        boolean shouldPoll = inventoryObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenPolledRecently() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        inventoryObserver.setLastPolledTimestamp(LocalDateTime.now());

        boolean shouldPoll = inventoryObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldReturnTrueWhenPollingIntervalHasPassed() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        inventoryObserver.setLastPolledTimestamp(LocalDateTime.now().minusSeconds(3601));

        boolean shouldPoll = inventoryObserver.shouldPoll();

        assertTrue(shouldPoll);
    }

    @Test
    void shouldReturnFalseWhenObserverIsInactive() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        inventoryObserver.deactivate();

        boolean shouldPoll = inventoryObserver.shouldPoll();

        assertFalse(shouldPoll);
    }

    @Test
    void shouldNotPollWhenShouldPollReturnsFalse() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        inventoryObserver.setLastPolledTimestamp(LocalDateTime.now());

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        verify(inventoryPort, never()).getInventorySnapshot();
        assertTrue(inventoryObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldFetchSnapshotWhenShouldPollReturnsTrue() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);
        when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        verify(inventoryPort).getInventorySnapshot();
        assertNotNull(inventoryObserver.getLastPolledTimestamp());
    }

    @Test
    void shouldGenerateEventWhenSnapshotIsFetched() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);
        when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        List<Object> domainEvents = inventoryObserver.getDomainEvents();
        assertEquals(1, domainEvents.size());
        assertInstanceOf(InventorySnapshotObservedEvent.class, domainEvents.get(0));

        InventorySnapshotObservedEvent event = (InventorySnapshotObservedEvent) domainEvents.get(0);
        assertEquals("observer-1", event.getObserverId());
        assertEquals(1, event.getSnapshots().size());
        assertEquals("SKU001", event.getSnapshots().get(0).getSku());
    }

    @Test
    void shouldUpdateLastPolledTimestampAfterSuccessfulPoll() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        LocalDateTime beforePoll = LocalDateTime.now();

        List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);
        when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        assertNotNull(inventoryObserver.getLastPolledTimestamp());
        assertTrue(
                inventoryObserver.getLastPolledTimestamp().isAfter(beforePoll)
                        || inventoryObserver.getLastPolledTimestamp().isEqual(beforePoll));
    }

    @Test
    void shouldClearAllDomainEvents() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);
        when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        inventoryObserver.clearDomainEvents();

        assertTrue(inventoryObserver.getDomainEvents().isEmpty());
    }

    @Test
    void shouldReturnUnmodifiableListOfDomainEvents() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);
        when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

        inventoryObserver.pollInventorySnapshot(inventoryPort);

        List<Object> events = inventoryObserver.getDomainEvents();

        assertThrows(UnsupportedOperationException.class, () -> events.add(new Object()));
    }

    @Test
    void shouldActivateObserver() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);
        inventoryObserver.deactivate();

        inventoryObserver.activate();

        assertTrue(inventoryObserver.isActive());
    }

    @Test
    void shouldDeactivateObserver() {
        ObservationRule observationRule = new ObservationRule(5.0, 3600);
        PollingInterval pollingInterval = new PollingInterval(3600);
        InventoryObserver inventoryObserver =
                new InventoryObserver("observer-1", observationRule, pollingInterval);

        inventoryObserver.deactivate();

        assertFalse(inventoryObserver.isActive());
    }

    private List<InventorySnapshotDto> createMockInventorySnapshotDtos(int count) {
        List<InventorySnapshotDto> dtos = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            InventorySnapshotDto dto = new InventorySnapshotDto();
            dto.setSku("SKU00" + (i + 1));
            dto.setProductName("Product " + (i + 1));
            dto.setWarehouseId("WH001");
            dto.setTotalQuantity(100 + i);
            dto.setReservedQuantity(10 + i);
            dto.setAvailableQuantity(90 + i);
            dto.setLocation("A-01-0" + (i + 1));
            dto.setUpdatedAt(
                    LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'")));
            dtos.add(dto);
        }
        return dtos;
    }
}
