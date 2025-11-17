package com.wei.orchestrator.wes.query.infrastructure;

import com.wei.orchestrator.wes.infrastructure.persistence.PickingTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PickingTaskQueryRepository extends JpaRepository<PickingTaskEntity, String> {

    @Query(
            value =
                    """
                    SELECT
                        pt.task_id,
                        pt.wes_task_id,
                        pt.order_id,
                        pt.origin,
                        pt.priority,
                        pt.status,
                        pt.created_at,
                        pt.canceled_at,
                        pt.completed_at,
                        pt.failure_reason,
                        ti.sku,
                        ti.quantity,
                        ti.location
                    FROM picking_tasks pt
                    LEFT JOIN task_items ti ON pt.task_id = ti.task_id
                    WHERE pt.task_id = :taskId
                    ORDER BY ti.id
                    """,
            nativeQuery = true)
    List<Object[]> findTaskDetailById(@Param("taskId") String taskId);

    @Query(
            value =
                    """
                    SELECT
                        pt.task_id,
                        pt.wes_task_id,
                        pt.order_id,
                        pt.origin,
                        pt.priority,
                        pt.status,
                        pt.created_at,
                        pt.canceled_at,
                        pt.completed_at,
                        COALESCE(COUNT(ti.id), 0) as item_count
                    FROM picking_tasks pt
                    LEFT JOIN task_items ti ON pt.task_id = ti.task_id
                    WHERE pt.order_id = :orderId
                    GROUP BY pt.task_id, pt.wes_task_id, pt.order_id, pt.origin, pt.priority, pt.status, pt.created_at, pt.completed_at
                    ORDER BY pt.created_at DESC
                    """,
            nativeQuery = true)
    List<Object[]> findTaskSummariesByOrderId(@Param("orderId") String orderId);
}
