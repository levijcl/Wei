package com.wei.orchestrator.order.infrastructure.repository;

import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, String> {

    @EntityGraph(attributePaths = {"orderLineItems"})
    @Override
    Optional<OrderEntity> findById(String id);

    @EntityGraph(attributePaths = {"orderLineItems"})
    List<OrderEntity> findByStatus(String status);

    @EntityGraph(attributePaths = {"orderLineItems"})
    @Query(
            "SELECT o FROM OrderEntity o WHERE o.status = 'SCHEDULED' AND o.scheduledPickupTime IS"
                    + " NOT NULL AND o.fulfillmentLeadTimeMinutes IS NOT NULL AND"
                    + " FUNCTION('TIMESTAMPADD', MINUTE, -o.fulfillmentLeadTimeMinutes,"
                    + " o.scheduledPickupTime) <= :currentTime")
    List<OrderEntity> findScheduledOrdersReadyForFulfillment(
            @Param("currentTime") LocalDateTime currentTime);
}
