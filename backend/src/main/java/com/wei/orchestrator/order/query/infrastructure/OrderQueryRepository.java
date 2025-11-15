package com.wei.orchestrator.order.query.infrastructure;

import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderQueryRepository extends JpaRepository<OrderEntity, String> {

    @Query(
            value =
                    "SELECT o.order_id, o.status, o.scheduled_pickup_time, o.shipment_carrier,"
                        + " o.shipment_tracking_number, COALESCE(stats.line_count, 0) as"
                        + " line_count, COALESCE(stats.total_quantity, 0) as total_quantity FROM"
                        + " orders o LEFT JOIN (SELECT order_id, COUNT(*) as line_count,"
                        + " SUM(quantity) as total_quantity FROM order_line_item GROUP BY order_id)"
                        + " stats ON o.order_id = stats.order_id WHERE o.status IN :statuses",
            countQuery = "SELECT COUNT(*) FROM orders WHERE status IN :statuses",
            nativeQuery = true)
    Page<Object[]> findOrderSummariesNative(
            @Param("statuses") List<String> statuses, Pageable pageable);

    @Query(
            value =
                    "SELECT o.order_id, o.status, o.scheduled_pickup_time, o.shipment_carrier,"
                        + " o.shipment_tracking_number, COALESCE(stats.line_count, 0) as"
                        + " line_count, COALESCE(stats.total_quantity, 0) as total_quantity FROM"
                        + " orders o LEFT JOIN (SELECT order_id, COUNT(*) as line_count,"
                        + " SUM(quantity) as total_quantity FROM order_line_item GROUP BY order_id)"
                        + " stats ON o.order_id = stats.order_id",
            countQuery = "SELECT COUNT(*) FROM orders",
            nativeQuery = true)
    Page<Object[]> findAllOrderSummariesNative(Pageable pageable);

    @EntityGraph(attributePaths = {"orderLineItems"})
    @Override
    Optional<OrderEntity> findById(String id);
}
