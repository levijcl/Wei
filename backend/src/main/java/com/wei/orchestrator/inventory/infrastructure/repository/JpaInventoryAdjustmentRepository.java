package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryAdjustmentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInventoryAdjustmentRepository
        extends JpaRepository<InventoryAdjustmentEntity, String> {
    List<InventoryAdjustmentEntity> findByStatus(AdjustmentStatus status);
}
