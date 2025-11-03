package com.wei.orchestrator.wes.domain.repository;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import java.util.List;
import java.util.Optional;

public interface PickingTaskRepository {
    PickingTask save(PickingTask pickingTask);

    Optional<PickingTask> findById(String taskId);

    List<PickingTask> findAll();

    List<PickingTask> findByOrderId(String orderId);

    List<PickingTask> findByStatus(TaskStatus status);

    List<PickingTask> findByWesTaskId(String wesTaskId);

    void deleteById(String taskId);

    boolean existsById(String taskId);

    boolean existsByWesTaskId(String wesTaskId);
}
