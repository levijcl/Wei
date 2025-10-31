package com.wei.orchestrator.order.infrastructure.repository;

import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, String> {

    @EntityGraph(attributePaths = {"orderLineItems"})
    @Override
    Optional<OrderEntity> findById(String id);
}
