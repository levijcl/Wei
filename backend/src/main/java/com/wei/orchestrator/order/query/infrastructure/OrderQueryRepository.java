package com.wei.orchestrator.order.query.infrastructure;

import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import com.wei.orchestrator.order.query.dto.OrderSummaryDto;
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
            "SELECT new com.wei.orchestrator.order.query.dto.OrderSummaryDto(o.orderId, o.status,"
                    + " o.scheduledPickupTime, o.shipmentCarrier, o.shipmentTrackingNumber) FROM"
                    + " OrderEntity o WHERE o.status IN :statuses")
    Page<OrderSummaryDto> findOrderSummaries(
            @Param("statuses") List<String> statuses, Pageable pageable);

    @Query(
            "SELECT new com.wei.orchestrator.order.query.dto.OrderSummaryDto(o.orderId, o.status,"
                    + " o.scheduledPickupTime, o.shipmentCarrier, o.shipmentTrackingNumber) FROM"
                    + " OrderEntity o")
    Page<OrderSummaryDto> findAllOrderSummaries(Pageable pageable);

    @EntityGraph(attributePaths = {"orderLineItems"})
    @Override
    Optional<OrderEntity> findById(String id);
}
