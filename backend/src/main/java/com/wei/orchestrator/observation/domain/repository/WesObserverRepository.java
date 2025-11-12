package com.wei.orchestrator.observation.domain.repository;

import com.wei.orchestrator.observation.domain.model.WesObserver;
import java.util.List;
import java.util.Optional;

public interface WesObserverRepository {
    WesObserver save(WesObserver wesObserver);

    Optional<WesObserver> findById(String observerId);

    List<WesObserver> findAllActive();

    void deleteById(String observerId);
}
