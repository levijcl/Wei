package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.domain.repository.InventoryAdjustmentRepository;
import com.wei.orchestrator.inventory.infrastructure.mapper.InventoryAdjustmentMapper;
import com.wei.orchestrator.inventory.infrastructure.persistence.DiscrepancyLogEntity;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryAdjustmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryAdjustmentRepositoryImpl implements InventoryAdjustmentRepository {

    private final JpaInventoryAdjustmentRepository jpaInventoryAdjustmentRepository;
    private final JpaDiscrepancyLogRepository jpaDiscrepancyLogRepository;

    public InventoryAdjustmentRepositoryImpl(
            JpaInventoryAdjustmentRepository jpaInventoryAdjustmentRepository,
            JpaDiscrepancyLogRepository jpaDiscrepancyLogRepository) {
        this.jpaInventoryAdjustmentRepository = jpaInventoryAdjustmentRepository;
        this.jpaDiscrepancyLogRepository = jpaDiscrepancyLogRepository;
    }

    @Override
    @Transactional
    public InventoryAdjustment save(InventoryAdjustment adjustment) {
        InventoryAdjustmentEntity entity = InventoryAdjustmentMapper.toEntity(adjustment);

        jpaDiscrepancyLogRepository.deleteAll(entity.getDiscrepancyLogs());
        entity.getDiscrepancyLogs().clear();

        List<DiscrepancyLogEntity> logEntities =
                InventoryAdjustmentMapper.toDiscrepancyLogEntities(
                        entity, adjustment.getDiscrepancyLogs());

        logEntities.forEach(entity::addDiscrepancyLog);

        InventoryAdjustmentEntity savedEntity = jpaInventoryAdjustmentRepository.save(entity);

        List<DiscrepancyLogEntity> savedLogEntities =
                jpaDiscrepancyLogRepository.findByAdjustment_AdjustmentId(
                        savedEntity.getAdjustmentId());

        return InventoryAdjustmentMapper.toDomain(savedEntity, savedLogEntities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryAdjustment> findById(String adjustmentId) {
        Optional<InventoryAdjustmentEntity> entityOpt =
                jpaInventoryAdjustmentRepository.findById(adjustmentId);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        List<DiscrepancyLogEntity> logEntities =
                jpaDiscrepancyLogRepository.findByAdjustment_AdjustmentId(adjustmentId);
        return Optional.of(InventoryAdjustmentMapper.toDomain(entityOpt.get(), logEntities));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryAdjustment> findByStatus(AdjustmentStatus status) {
        List<InventoryAdjustmentEntity> entities =
                jpaInventoryAdjustmentRepository.findByStatus(status);
        return entities.stream()
                .map(
                        entity -> {
                            List<DiscrepancyLogEntity> logEntities =
                                    jpaDiscrepancyLogRepository.findByAdjustment_AdjustmentId(
                                            entity.getAdjustmentId());
                            return InventoryAdjustmentMapper.toDomain(entity, logEntities);
                        })
                .collect(Collectors.toList());
    }
}
