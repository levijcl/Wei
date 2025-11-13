package com.wei.orchestrator.observation.domain.repository;

import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import java.util.List;
import java.util.Optional;

public interface InventoryObserverRepository {
    InventoryObserver save(InventoryObserver inventoryObserver);

    Optional<InventoryObserver> findById(String observerId);

    List<InventoryObserver> findAllActive();

    void deleteById(String observerId);
}
