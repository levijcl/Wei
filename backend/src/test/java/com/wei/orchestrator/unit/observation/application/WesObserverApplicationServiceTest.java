package com.wei.orchestrator.unit.observation.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.application.WesObserverApplicationService;
import com.wei.orchestrator.observation.application.command.CreateWesObserverCommand;
import com.wei.orchestrator.observation.application.command.PollWesTaskStatusCommand;
import com.wei.orchestrator.observation.domain.event.WesTaskStatusUpdatedEvent;
import com.wei.orchestrator.observation.domain.model.WesObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.TaskEndpoint;
import com.wei.orchestrator.observation.domain.repository.WesObserverRepository;
import com.wei.orchestrator.shared.domain.model.valueobject.TriggerContext;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesTaskDto;
import java.time.LocalDateTime;
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
class WesObserverApplicationServiceTest {

    @Mock private WesObserverRepository wesObserverRepository;

    @Mock private WesPort wesPort;

    @Mock private PickingTaskRepository pickingTaskRepository;

    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WesObserverApplicationService wesObserverApplicationService;

    @Nested
    class createWesObserverTest {

        @Test
        void shouldCreateWesObserverSuccessfully() {
            CreateWesObserverCommand command =
                    new CreateWesObserverCommand(
                            "observer-1", "http://localhost:8080/api", "token123", 60);

            when(wesObserverRepository.save(any(WesObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String observerId = wesObserverApplicationService.createWesObserver(command);

            assertEquals("observer-1", observerId);
            verify(wesObserverRepository, times(1)).save(any(WesObserver.class));
        }

        @Test
        void shouldCallRepositorySaveExactlyOnce() {
            CreateWesObserverCommand command =
                    new CreateWesObserverCommand(
                            "observer-2", "http://localhost:9090/api", "token456", 120);

            when(wesObserverRepository.save(any(WesObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            wesObserverApplicationService.createWesObserver(command);

            verify(wesObserverRepository, times(1)).save(any(WesObserver.class));
            verifyNoMoreInteractions(wesObserverRepository);
        }

        @Test
        void shouldCreateObserverWithCorrectTaskEndpoint() {
            CreateWesObserverCommand command =
                    new CreateWesObserverCommand(
                            "observer-3", "http://wes.example.com/api", "prodtoken", 90);

            ArgumentCaptor<WesObserver> captor = ArgumentCaptor.forClass(WesObserver.class);
            when(wesObserverRepository.save(any(WesObserver.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            wesObserverApplicationService.createWesObserver(command);

            verify(wesObserverRepository).save(captor.capture());
            WesObserver savedObserver = captor.getValue();
            assertEquals("observer-3", savedObserver.getObserverId());
            assertTrue(savedObserver.isActive());
        }
    }

    @Nested
    class pollWesTaskStatusTest {

        @Test
        void shouldPollWesTaskStatusSuccessfully() {
            WesObserver mockObserver = createMockWesObserver("observer-1");
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-1");

            List<String> existingWesTaskIds = List.of("WES-TASK-001");
            Map<String, TaskStatus> existingTaskStatuses = new HashMap<>();
            existingTaskStatuses.put("WES-TASK-001", TaskStatus.PENDING);

            List<WesTaskDto> mockTasks = createMockWesTasks(1);
            mockTasks.get(0).setStatus("IN_PROGRESS");

            when(wesObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(existingWesTaskIds);
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(existingTaskStatuses);
            when(wesPort.pollAllTasks()).thenReturn(mockTasks);

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            verify(wesObserverRepository).findById("observer-1");
            verify(wesObserverRepository).save(mockObserver);
            verify(eventPublisher, times(1)).publishEvent(any(WesTaskStatusUpdatedEvent.class));
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFound() {
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("non-existent");

            when(wesObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                wesObserverApplicationService.pollWesTaskStatus(
                                        command, TriggerContext.scheduled("WesObserver"));
                            });

            assertTrue(exception.getMessage().contains("WesObserver not found"));
            verify(wesObserverRepository).findById("non-existent");
            verify(wesObserverRepository, never()).save(any());
        }

        @Test
        void shouldNotPollWhenShouldPollReturnsFalse() {
            WesObserver mockObserver = createMockWesObserver("observer-2");
            mockObserver.setLastPolledTimestamp(LocalDateTime.now());
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-2");

            when(wesObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            verify(wesObserverRepository).findById("observer-2");
            verify(pickingTaskRepository, never()).findAllWesTaskIds();
            verify(wesPort, never()).pollAllTasks();
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        void shouldPublishEventsAfterSuccessfulSave() {
            WesObserver mockObserver = createMockWesObserver("observer-3");
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-3");

            List<String> existingWesTaskIds = List.of("WES-TASK-001", "WES-TASK-002");
            Map<String, TaskStatus> existingTaskStatuses = new HashMap<>();
            existingTaskStatuses.put("WES-TASK-001", TaskStatus.PENDING);
            existingTaskStatuses.put("WES-TASK-002", TaskStatus.IN_PROGRESS);

            List<WesTaskDto> mockTasks = createMockWesTasks(2);
            mockTasks.get(0).setStatus("COMPLETED");
            mockTasks.get(1).setStatus("FAILED");

            when(wesObserverRepository.findById("observer-3"))
                    .thenReturn(Optional.of(mockObserver));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(existingWesTaskIds);
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(existingTaskStatuses);
            when(wesPort.pollAllTasks()).thenReturn(mockTasks);

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            verify(wesObserverRepository).save(mockObserver);
            verify(eventPublisher, times(2)).publishEvent(any(WesTaskStatusUpdatedEvent.class));
        }

        @Test
        void shouldNotPublishEventsWhenNoStatusChanges() {
            WesObserver mockObserver = createMockWesObserver("observer-4");
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-4");

            List<String> existingWesTaskIds = List.of("WES-TASK-001");
            Map<String, TaskStatus> existingTaskStatuses = new HashMap<>();
            existingTaskStatuses.put("WES-TASK-001", TaskStatus.PENDING);

            List<WesTaskDto> mockTasks = createMockWesTasks(1);
            mockTasks.get(0).setStatus("PENDING");

            when(wesObserverRepository.findById("observer-4"))
                    .thenReturn(Optional.of(mockObserver));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(existingWesTaskIds);
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(existingTaskStatuses);
            when(wesPort.pollAllTasks()).thenReturn(mockTasks);

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            verify(wesObserverRepository).save(mockObserver);
            verify(eventPublisher, never()).publishEvent(any(WesTaskStatusUpdatedEvent.class));
        }

        @Test
        void shouldClearDomainEventsAfterPublishing() {
            WesObserver mockObserver = createMockWesObserver("observer-5");
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-5");

            List<String> existingWesTaskIds = List.of("WES-TASK-001");
            Map<String, TaskStatus> existingTaskStatuses = new HashMap<>();
            existingTaskStatuses.put("WES-TASK-001", TaskStatus.PENDING);

            List<WesTaskDto> mockTasks = createMockWesTasks(1);
            mockTasks.get(0).setStatus("COMPLETED");

            when(wesObserverRepository.findById("observer-5"))
                    .thenReturn(Optional.of(mockObserver));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(existingWesTaskIds);
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(existingTaskStatuses);
            when(wesPort.pollAllTasks()).thenReturn(mockTasks);

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            assertTrue(mockObserver.getDomainEvents().isEmpty());
        }

        @Test
        void shouldQueryPickingTaskRepositoryForExistingData() {
            WesObserver mockObserver = createMockWesObserver("observer-6");
            PollWesTaskStatusCommand command = new PollWesTaskStatusCommand("observer-6");

            when(wesObserverRepository.findById("observer-6"))
                    .thenReturn(Optional.of(mockObserver));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(Collections.emptyList());
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(Collections.emptyMap());
            when(wesPort.pollAllTasks()).thenReturn(Collections.emptyList());

            wesObserverApplicationService.pollWesTaskStatus(
                    command, TriggerContext.scheduled("WesObserver"));

            verify(pickingTaskRepository).findAllWesTaskIds();
            verify(pickingTaskRepository).findAllTaskStatusesByWesTaskId();
        }
    }

    @Nested
    class pollAllActiveObserversTest {

        @Test
        void shouldPollAllActiveObservers() {
            WesObserver observer1 = createMockWesObserver("observer-1");
            WesObserver observer2 = createMockWesObserver("observer-2");
            List<WesObserver> activeObservers = Arrays.asList(observer1, observer2);

            when(wesObserverRepository.findAllActive()).thenReturn(activeObservers);
            when(wesObserverRepository.findById("observer-1")).thenReturn(Optional.of(observer1));
            when(wesObserverRepository.findById("observer-2")).thenReturn(Optional.of(observer2));
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(Collections.emptyList());
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(Collections.emptyMap());
            when(wesPort.pollAllTasks()).thenReturn(Collections.emptyList());

            wesObserverApplicationService.pollAllActiveObservers();

            verify(wesObserverRepository).findAllActive();
            verify(wesObserverRepository, times(2)).save(any(WesObserver.class));
        }

        @Test
        void shouldHandleEmptyActiveObserversList() {
            when(wesObserverRepository.findAllActive()).thenReturn(new ArrayList<>());

            wesObserverApplicationService.pollAllActiveObservers();

            verify(wesObserverRepository).findAllActive();
            verify(wesPort, never()).pollAllTasks();
        }

        @Test
        void shouldCallPollWesTaskStatusForEachActiveObserver() {
            WesObserver observer1 = createMockWesObserver("observer-1");
            WesObserver observer2 = createMockWesObserver("observer-2");
            WesObserver observer3 = createMockWesObserver("observer-3");
            List<WesObserver> activeObservers = Arrays.asList(observer1, observer2, observer3);

            when(wesObserverRepository.findAllActive()).thenReturn(activeObservers);
            when(wesObserverRepository.findById(anyString()))
                    .thenAnswer(
                            invocation -> {
                                String id = invocation.getArgument(0);
                                return activeObservers.stream()
                                        .filter(obs -> obs.getObserverId().equals(id))
                                        .findFirst();
                            });
            when(pickingTaskRepository.findAllWesTaskIds()).thenReturn(Collections.emptyList());
            when(pickingTaskRepository.findAllTaskStatusesByWesTaskId())
                    .thenReturn(Collections.emptyMap());
            when(wesPort.pollAllTasks()).thenReturn(Collections.emptyList());

            wesObserverApplicationService.pollAllActiveObservers();

            verify(wesObserverRepository).findById("observer-1");
            verify(wesObserverRepository).findById("observer-2");
            verify(wesObserverRepository).findById("observer-3");
            verify(wesObserverRepository, times(3)).save(any(WesObserver.class));
        }
    }

    @Nested
    class activateObserverTest {

        @Test
        void shouldActivateObserver() {
            WesObserver mockObserver = createMockWesObserver("observer-1");
            mockObserver.deactivate();

            when(wesObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            wesObserverApplicationService.activateObserver("observer-1");

            assertTrue(mockObserver.isActive());
            verify(wesObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForActivation() {
            when(wesObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                wesObserverApplicationService.activateObserver("non-existent");
                            });

            assertTrue(exception.getMessage().contains("WesObserver not found"));
        }

        @Test
        void shouldSaveObserverAfterActivation() {
            WesObserver mockObserver = createMockWesObserver("observer-2");
            mockObserver.deactivate();

            when(wesObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            wesObserverApplicationService.activateObserver("observer-2");

            verify(wesObserverRepository).findById("observer-2");
            verify(wesObserverRepository).save(mockObserver);
        }
    }

    @Nested
    class deactivateObserverTest {

        @Test
        void shouldDeactivateObserver() {
            WesObserver mockObserver = createMockWesObserver("observer-1");

            when(wesObserverRepository.findById("observer-1"))
                    .thenReturn(Optional.of(mockObserver));

            wesObserverApplicationService.deactivateObserver("observer-1");

            assertFalse(mockObserver.isActive());
            verify(wesObserverRepository).save(mockObserver);
        }

        @Test
        void shouldThrowExceptionWhenObserverNotFoundForDeactivation() {
            when(wesObserverRepository.findById("non-existent")).thenReturn(Optional.empty());

            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> {
                                wesObserverApplicationService.deactivateObserver("non-existent");
                            });

            assertTrue(exception.getMessage().contains("WesObserver not found"));
        }

        @Test
        void shouldSaveObserverAfterDeactivation() {
            WesObserver mockObserver = createMockWesObserver("observer-2");

            when(wesObserverRepository.findById("observer-2"))
                    .thenReturn(Optional.of(mockObserver));

            wesObserverApplicationService.deactivateObserver("observer-2");

            verify(wesObserverRepository).findById("observer-2");
            verify(wesObserverRepository).save(mockObserver);
        }
    }

    private WesObserver createMockWesObserver(String observerId) {
        return new WesObserver(
                observerId,
                new TaskEndpoint("http://localhost:8080/api", "token123"),
                new PollingInterval(60));
    }

    private List<WesTaskDto> createMockWesTasks(int count) {
        List<WesTaskDto> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            WesTaskDto task = new WesTaskDto();
            task.setTaskId("WES-TASK-00" + (i + 1));
            task.setTaskType("PICKING");
            task.setOrderId("ORDER-" + (i + 1));
            task.setWarehouseId("WH001");
            task.setPriority(5);
            task.setStatus("PENDING");
            tasks.add(task);
        }
        return tasks;
    }
}
