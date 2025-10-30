package com.wei.orchestrator.order.infrastructure.repository;

import com.wei.orchestrator.order.infrastructure.persistence.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, String> {}
