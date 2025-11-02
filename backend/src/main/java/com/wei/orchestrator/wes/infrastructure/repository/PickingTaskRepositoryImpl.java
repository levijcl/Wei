package com.wei.orchestrator.wes.infrastructure.repository;

import com.wei.orchestrator.wes.domain.model.PickingTask;
import com.wei.orchestrator.wes.domain.model.valueobject.TaskStatus;
import com.wei.orchestrator.wes.domain.repository.PickingTaskRepository;
import com.wei.orchestrator.wes.infrastructure.mapper.PickingTaskMapper;
import com.wei.orchestrator.wes.infrastructure.persistence.PickingTaskEntity;
import com.wei.orchestrator.wes.infrastructure.persistence.TaskItemEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PickingTaskRepositoryImpl implements PickingTaskRepository {

    private final JpaPickingTaskRepository jpaPickingTaskRepository;
    private final JpaTaskItemRepository jpaTaskItemRepository;

    public PickingTaskRepositoryImpl(
            JpaPickingTaskRepository jpaPickingTaskRepository,
            JpaTaskItemRepository jpaTaskItemRepository) {
        this.jpaPickingTaskRepository = jpaPickingTaskRepository;
        this.jpaTaskItemRepository = jpaTaskItemRepository;
    }

    @Override
    @Transactional
    public PickingTask save(PickingTask pickingTask) {
        PickingTaskEntity entity = PickingTaskMapper.toEntity(pickingTask);
        PickingTaskEntity savedEntity = jpaPickingTaskRepository.save(entity);

        jpaTaskItemRepository.deleteByTaskId(pickingTask.getTaskId());

        List<TaskItemEntity> itemEntities =
                PickingTaskMapper.toTaskItemEntities(
                        pickingTask.getTaskId(), pickingTask.getItems());
        jpaTaskItemRepository.saveAll(itemEntities);

        return PickingTaskMapper.toDomain(savedEntity, itemEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PickingTask> findById(String taskId) {
        Optional<PickingTaskEntity> entityOpt = jpaPickingTaskRepository.findById(taskId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        List<TaskItemEntity> itemEntities = jpaTaskItemRepository.findByTaskId(taskId);
        PickingTask domain = PickingTaskMapper.toDomain(entityOpt.get(), itemEntities);
        return Optional.of(domain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickingTask> findAll() {
        List<PickingTaskEntity> entities = jpaPickingTaskRepository.findAll();
        return entities.stream()
                .map(
                        entity -> {
                            List<TaskItemEntity> itemEntities =
                                    jpaTaskItemRepository.findByTaskId(entity.getTaskId());
                            return PickingTaskMapper.toDomain(entity, itemEntities);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickingTask> findByOrderId(String orderId) {
        List<PickingTaskEntity> entities = jpaPickingTaskRepository.findByOrderId(orderId);
        return entities.stream()
                .map(
                        entity -> {
                            List<TaskItemEntity> itemEntities =
                                    jpaTaskItemRepository.findByTaskId(entity.getTaskId());
                            return PickingTaskMapper.toDomain(entity, itemEntities);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickingTask> findByStatus(TaskStatus status) {
        List<PickingTaskEntity> entities = jpaPickingTaskRepository.findByStatus(status);
        return entities.stream()
                .map(
                        entity -> {
                            List<TaskItemEntity> itemEntities =
                                    jpaTaskItemRepository.findByTaskId(entity.getTaskId());
                            return PickingTaskMapper.toDomain(entity, itemEntities);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PickingTask> findByWesTaskId(String wesTaskId) {
        List<PickingTaskEntity> entities = jpaPickingTaskRepository.findByWesTaskId(wesTaskId);
        return entities.stream()
                .map(
                        entity -> {
                            List<TaskItemEntity> itemEntities =
                                    jpaTaskItemRepository.findByTaskId(entity.getTaskId());
                            return PickingTaskMapper.toDomain(entity, itemEntities);
                        })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(String taskId) {
        jpaTaskItemRepository.deleteByTaskId(taskId);
        jpaPickingTaskRepository.deleteById(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String taskId) {
        return jpaPickingTaskRepository.existsById(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWesTaskId(String wesTaskId) {
        return jpaPickingTaskRepository.existsByWesTaskId(wesTaskId);
    }
}
