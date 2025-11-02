package com.wei.orchestrator.wes.infrastructure.repository;

import com.wei.orchestrator.wes.infrastructure.persistence.TaskItemEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaTaskItemRepository extends JpaRepository<TaskItemEntity, Long> {
    List<TaskItemEntity> findByTaskId(String taskId);

    void deleteByTaskId(String taskId);
}
