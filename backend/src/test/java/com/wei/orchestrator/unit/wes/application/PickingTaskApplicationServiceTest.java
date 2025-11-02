package com.wei.orchestrator.unit.wes.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.wes.application.PickingTaskApplicationService;
import com.wei.orchestrator.wes.application.command.CreatePickingTaskForOrderCommand;
import com.wei.orchestrator.wes.application.command.dto.TaskItemDto;
import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import com.wei.orchestrator.wes.domain.port.WesPort;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PickingTaskApplicationServiceTest {
    @Mock private PickingTaskRepository pickingTaskRepository;
    @Mock private WesPort wesPort;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private PickingTaskApplicationService pickingTaskApplicationService;

    @Nested
    class createPickingTaskForOrderTest {
        @Test
        void shouldCreatePickingTaskForOrderSuccessfully() {
            when(pickingTaskRepository.save(any(PickingTask.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(wesPort.submitPickingTask(any(PickingTask.class)))
                    .thenReturn(WesTaskId.of("WES_TASK_001"));

            List<TaskItemDto> items = List.of(new TaskItemDto("SKU001", 10, "WH001"));
            CreatePickingTaskForOrderCommand command =
                    new CreatePickingTaskForOrderCommand("ORDER_001", items, 1);

            String expectedTaskId =
                    pickingTaskApplicationService.createPickingTaskForOrder(command);

            Assertions.assertNotNull(expectedTaskId);

            verify(pickingTaskRepository, times(2)).save(any(PickingTask.class));
            verify(wesPort).submitPickingTask(any(PickingTask.class));
            verify(eventPublisher, times(2)).publishEvent(any(Object.class));
        }
    }
}
