package com.wei.orchestrator.wes.domain.repository;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PickingTaskRepository {
    PickingTask save(PickingTask pickingTask);

    Optional<PickingTask> findById(String taskId);

    List<PickingTask> findByOrderId(String orderId);

    List<PickingTask> findByStatus(TaskStatus status);

    List<PickingTask> findByWesTaskId(String wesTaskId);

    void deleteById(String taskId);

    List<String> findAllWesTaskIds();

    Map<String, TaskStatus> findAllTaskStatusesByWesTaskId();
}
