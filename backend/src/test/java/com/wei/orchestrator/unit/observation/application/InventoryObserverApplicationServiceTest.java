package com.wei.orchestrator.unit.observation.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.infrastructure.adapter.dto.InventorySnapshotDto;
import com.wei.orchestrator.observation.application.InventoryObserverApplicationService;
import com.wei.orchestrator.observation.application.command.CreateInventoryObserverCommand;
import com.wei.orchestrator.observation.application.command.PollInventorySnapshotCommand;
import com.wei.orchestrator.observation.domain.event.InventorySnapshotObservedEvent;
import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationRule;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.repository.InventoryObserverRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InventoryObserverApplicationServiceTest {

    @Mock private InventoryObserverRepository inventoryObserverRepository;

    @Mock private InventoryPort inventoryPort;

    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private InventoryObserverApplicationService inventoryObserverApplicationService;

    @Nested
    class createInventoryObserverTest {

        @Test
        void shouldCreateInventoryObserverSuccessfully() {
            CreateInventoryObserverCommand command =
                    new CreateInventoryObserverCommand("observer-1", 5.0, 3600, 3600);

            when(inventoryObserverRepository.save(any(InventoryObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String observerId =
                    inventoryObserverApplicationService.createInventoryObserver(command);

            assertEquals("observer-1", observerId);
            verify(inventoryObserverRepository, times(1)).save(any(InventoryObserver.class));
        }

        @Test
        void shouldCallRepositorySaveExactlyOnce() {
            CreateInventoryObserverCommand command =
                    new CreateInventoryObserverCommand("observer-2", 10.0, 7200, 7200);

            when(inventoryObserverRepository.save(any(InventoryObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            inventoryObserverApplicationService.createInventoryObserver(command);

            verify(inventoryObserverRepository, times(1)).save(any(InventoryObserver.class));
            verifyNoMoreInteractions(inventoryObserverRepository);
        }

        @Test
        void shouldCreateObserverWithCorrectObservationRule() {
            CreateInventoryObserverCommand command =
                    new CreateInventoryObserverCommand("observer-3", 8.5, 1800, 1800);

            ArgumentCaptor<InventoryObserver> captor =
                    ArgumentCaptor.forClass(InventoryObserver.class);
            when(inventoryObserverRepository.save(any(InventoryObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            inventoryObserverApplicationService.createInventoryObserver(command);

            verify(inventoryObserverRepository).save(captor.capture());
            InventoryObserver savedObserver = captor.getValue();
            assertEquals("observer-3", savedObserver.getObserverId());
            assertTrue(savedObserver.isActive());
        }
    }

    @Nested
    class pollInventorySnapshotTest {

        @Test
        void shouldPollInventorySnapshotSuccessfully() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-1");
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("observer-1");

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollInventorySnapshot(command);

            verify(inventoryObserverRepository).findById("observer-1");
            verify(inventoryObserverRepository).save(mockObserver);
            verify(eventPublisher, times(1))
                    .publishEvent(any(InventorySnapshotObservedEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFound() {
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("non-existent");

            when(inventoryObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                inventoryObserverApplicationService.pollInventorySnapshot(command);
                            });

            assertTrue(exception.getMessage().contains("InventoryObserver not found"));
            verify(inventoryObserverRepository).findById("non-existent");
            verify(inventoryObserverRepository, never()).save(any());
        }

        @Test
        void shouldNotPollWhenShouldPollReturnsFalse() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-2");
            mockObserver.setLastPolledTimestamp(LocalDateTime.now());
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("observer-2");

            when(inventoryObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            inventoryObserverApplicationService.pollInventorySnapshot(command);

            verify(inventoryObserverRepository).findById("observer-2");
            verify(inventoryPort, never()).getInventorySnapshot();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void shouldPublishEventAfterSuccessfulSave() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-3");
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("observer-3");

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findById("observer-3"))
                    .thenReturn(Optional.of(mockObserver));
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollInventorySnapshot(command);

            verify(inventoryObserverRepository).save(mockObserver);
            verify(eventPublisher, times(1))
                    .publishEvent(any(InventorySnapshotObservedEvent.class));
        }

        @Test
        void shouldClearDomainEventsAfterPublishing() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-4");
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("observer-4");

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findById("observer-4"))
                    .thenReturn(Optional.of(mockObserver));
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollInventorySnapshot(command);

            assertTrue(mockObserver.getDomainEvents().isEmpty());
        }

        @Test
        void shouldCallInventoryPortToGetSnapshot() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-5");
            PollInventorySnapshotCommand command = new PollInventorySnapshotCommand("observer-5");

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findById("observer-5"))
                    .thenReturn(Optional.of(mockObserver));
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollInventorySnapshot(command);

            verify(inventoryPort).getInventorySnapshot();
        }
    }

    @Nested
    class pollAllActiveObserversTest {

        @Test
        void shouldPollAllActiveObservers() {
            InventoryObserver observer1 = createMockInventoryObserver("observer-1");
            InventoryObserver observer2 = createMockInventoryObserver("observer-2");
            List<InventoryObserver> activeObservers = Arrays.asList(observer1, observer2);

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findAllActive()).thenReturn(activeObservers);
            when(inventoryObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(observer1));
            when(inventoryObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(observer2));
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollAllActiveObservers();

            verify(inventoryObserverRepository).findAllActive();
            verify(inventoryObserverRepository, times(2)).save(any(InventoryObserver.class));
        }

        @Test
        void shouldHandleEmptyActiveObserversList() {
            when(inventoryObserverRepository.findAllActive()).thenReturn(new ArrayList<>());

            inventoryObserverApplicationService.pollAllActiveObservers();

            verify(inventoryObserverRepository).findAllActive();
            verify(inventoryPort, never()).getInventorySnapshot();
        }

        @Test
        void shouldCallPollInventorySnapshotForEachActiveObserver() {
            InventoryObserver observer1 = createMockInventoryObserver("observer-1");
            InventoryObserver observer2 = createMockInventoryObserver("observer-2");
            InventoryObserver observer3 = createMockInventoryObserver("observer-3");
            List<InventoryObserver> activeObservers =
                    Arrays.asList(observer1, observer2, observer3);

            List<InventorySnapshotDto> mockDtos = createMockInventorySnapshotDtos(1);

            when(inventoryObserverRepository.findAllActive()).thenReturn(activeObservers);
            when(inventoryObserverRepository.findById(anyString()))
                    .thenAnswer(
                            invocation -> {
                                String id = invocation.getArgument(0);
                                return activeObservers.stream()
                                        .filter(obs -> obs.getObserverId().equals(id))
                                        .findFirst();
                            });
            when(inventoryPort.getInventorySnapshot()).thenReturn(mockDtos);

            inventoryObserverApplicationService.pollAllActiveObservers();

            verify(inventoryObserverRepository).findById("observer-1");
            verify(inventoryObserverRepository).findById("observer-2");
            verify(inventoryObserverRepository).findById("observer-3");
            verify(inventoryObserverRepository, times(3)).save(any(InventoryObserver.class));
        }
    }

    @Nested
    class activateObserverTest {

        @Test
        void shouldActivateObserver() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-1");
            mockObserver.deactivate();

            when(inventoryObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            inventoryObserverApplicationService.activateObserver("observer-1");

            assertTrue(mockObserver.isActive());
            verify(inventoryObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForActivation() {
            when(inventoryObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                inventoryObserverApplicationService.activateObserver(
                                        "non-existent");
                            });

            assertTrue(exception.getMessage().contains("InventoryObserver not found"));
        }

        @Test
        void shouldSaveObserverAfterActivation() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-2");
            mockObserver.deactivate();

            when(inventoryObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            inventoryObserverApplicationService.activateObserver("observer-2");

            verify(inventoryObserverRepository).findById("observer-2");
            verify(inventoryObserverRepository).save(mockObserver);
        }
    }

    @Nested
    class deactivateObserverTest {

        @Test
        void shouldDeactivateObserver() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-1");

            when(inventoryObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            inventoryObserverApplicationService.deactivateObserver("observer-1");

            assertFalse(mockObserver.isActive());
            verify(inventoryObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForDeactivation() {
            when(inventoryObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                inventoryObserverApplicationService.deactivateObserver(
                                        "non-existent");
                            });

            assertTrue(exception.getMessage().contains("InventoryObserver not found"));
        }

        @Test
        void shouldSaveObserverAfterDeactivation() {
            InventoryObserver mockObserver = createMockInventoryObserver("observer-2");

            when(inventoryObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            inventoryObserverApplicationService.deactivateObserver("observer-2");

            verify(inventoryObserverRepository).findById("observer-2");
            verify(inventoryObserverRepository).save(mockObserver);
        }
    }

    private InventoryObserver createMockInventoryObserver(String observerId) {
        return new InventoryObserver(
                observerId, new ObservationRule(5.0, 3600), new PollingInterval(3600));
    }

    private List<InventorySnapshotDto> createMockInventorySnapshotDtos(int count) {
        List<InventorySnapshotDto> dtos = new ArrayList<>();
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
