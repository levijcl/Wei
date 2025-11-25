package com.wei.orchestrator.inventory.query.infrastructure;

import com.wei.orchestrator.inventory.infrastructure.persistence.InventoryAdjustmentEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryAdjustmentQueryRepository
        extends JpaRepository<InventoryAdjustmentEntity, String> {

    @Query(
            value =
                    "SELECT ia.adjustment_id, ia.status, ia.created_at, ia.processed_at, "
                            + "COALESCE(stats.discrepancy_count, 0) as discrepancy_count, "
                            + "COALESCE(stats.total_difference, 0) as total_difference "
                            + "FROM inventory_adjustments ia "
                            + "LEFT JOIN ("
                            + "  SELECT adjustment_id, COUNT(*) as discrepancy_count, "
                            + "  SUM(ABS(difference)) as total_difference "
                            + "  FROM discrepancy_logs "
                            + "  GROUP BY adjustment_id"
                            + ") stats ON ia.adjustment_id = stats.adjustment_id "
                            + "WHERE ia.status IN :statuses "
                            + "ORDER BY ia.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM inventory_adjustments WHERE status IN :statuses",
            nativeQuery = true)
    Page<Object[]> findAdjustmentSummariesByStatusNative(
            @Param("statuses") List<String> statuses, Pageable pageable);

    @Query(
            value =
                    "SELECT ia.adjustment_id, ia.status, ia.created_at, ia.processed_at, "
                            + "COALESCE(stats.discrepancy_count, 0) as discrepancy_count, "
                            + "COALESCE(stats.total_difference, 0) as total_difference "
                            + "FROM inventory_adjustments ia "
                            + "LEFT JOIN ("
                            + "  SELECT adjustment_id, COUNT(*) as discrepancy_count, "
                            + "  SUM(ABS(difference)) as total_difference "
                            + "  FROM discrepancy_logs "
                            + "  GROUP BY adjustment_id"
                            + ") stats ON ia.adjustment_id = stats.adjustment_id "
                            + "ORDER BY ia.created_at DESC",
            countQuery = "SELECT COUNT(*) FROM inventory_adjustments",
            nativeQuery = true)
    Page<Object[]> findAllAdjustmentSummariesNative(Pageable pageable);

    @EntityGraph(attributePaths = {"discrepancyLogs"})
    @Override
    java.util.Optional<InventoryAdjustmentEntity> findById(String id);
}
