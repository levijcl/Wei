package com.wei.orchestrator.wes.infrastructure.repository;

import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.infrastructure.persistence.PickingTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaPickingTaskRepository extends JpaRepository<PickingTaskEntity, String> {
    List<PickingTaskEntity> findByOrderId(String orderId);

    List<PickingTaskEntity> findByStatus(TaskStatus status);

    List<PickingTaskEntity> findByWesTaskId(String wesTaskId);

    boolean existsByWesTaskId(String wesTaskId);
}
