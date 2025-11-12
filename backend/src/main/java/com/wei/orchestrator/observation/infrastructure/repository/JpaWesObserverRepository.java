package com.wei.orchestrator.observation.infrastructure.repository;

import com.wei.orchestrator.observation.infrastructure.persistence.WesObserverEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWesObserverRepository extends JpaRepository<WesObserverEntity, String> {
    List<WesObserverEntity> findByActiveTrue();
}
