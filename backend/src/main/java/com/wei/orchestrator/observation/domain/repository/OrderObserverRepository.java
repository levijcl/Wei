package com.wei.orchestrator.observation.domain.repository;

import com.wei.orchestrator.observation.domain.model.OrderObserver;
import java.util.List;
import java.util.Optional;

public interface OrderObserverRepository {
    OrderObserver save(OrderObserver orderObserver);

    Optional<OrderObserver> findById(String observerId);

    List<OrderObserver> findAllActive();

    void deleteById(String observerId);
}
