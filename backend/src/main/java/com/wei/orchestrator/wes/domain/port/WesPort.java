package com.wei.orchestrator.wes.domain.port;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.model.valueobject.WesTaskId;
import java.util.Optional;

public interface WesPort {
    WesTaskId submitPickingTask(PickingTask task);

    Optional<TaskStatus> getTaskStatus(WesTaskId wesTaskId);

    void updateTaskPriority(WesTaskId wesTaskId, int priority);

    void cancelTask(WesTaskId wesTaskId);
}
