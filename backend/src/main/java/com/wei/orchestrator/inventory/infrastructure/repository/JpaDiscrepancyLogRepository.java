package com.wei.orchestrator.inventory.infrastructure.repository;

import com.wei.orchestrator.inventory.infrastructure.persistence.DiscrepancyLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaDiscrepancyLogRepository extends JpaRepository<DiscrepancyLogEntity, Long> {
    List<DiscrepancyLogEntity> findByAdjustment_AdjustmentId(String adjustmentId);
}
