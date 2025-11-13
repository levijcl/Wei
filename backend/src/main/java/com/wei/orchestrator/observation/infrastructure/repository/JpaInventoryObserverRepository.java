package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.infrastructure.persistence.InventoryObserverEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaInventoryObserverRepository
        extends JpaRepository<InventoryObserverEntity, String> {
    List<InventoryObserverEntity> findByActiveTrue();
}
