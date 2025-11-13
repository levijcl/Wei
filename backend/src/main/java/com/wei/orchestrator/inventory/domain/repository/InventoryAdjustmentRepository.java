package com.wei.orchestrator.inventory.domain.repository;

import com.wei.orchestrator.inventory.domain.model.InventoryAdjustment;
import com.wei.orchestrator.inventory.domain.model.valueobject.AdjustmentStatus;
import java.util.List;
import java.util.Optional;

public interface InventoryAdjustmentRepository {
    InventoryAdjustment save(InventoryAdjustment adjustment);

    Optional<InventoryAdjustment> findById(String adjustmentId);

    List<InventoryAdjustment> findByStatus(AdjustmentStatus status);
}
