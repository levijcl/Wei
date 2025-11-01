package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.infrastructure.persistence.OrderObserverEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaOrderObserverRepository extends JpaRepository<OrderObserverEntity, String> {
    List<OrderObserverEntity> findByActiveTrue();
}
