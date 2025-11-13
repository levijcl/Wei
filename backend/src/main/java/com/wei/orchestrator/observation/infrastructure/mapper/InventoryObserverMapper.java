package com.wei.orchestrator.observation.infrastructure.mapper;

import com.wei.orchestrator.observation.domain.model.InventoryObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationRule;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.infrastructure.persistence.InventoryObserverEntity;

public class InventoryObserverMapper {

    public static InventoryObserverEntity toEntity(InventoryObserver domain) {
        if (domain == null) {
            return null;
        }

        InventoryObserverEntity entity = new InventoryObserverEntity();
        entity.setObserverId(domain.getObserverId());

        if (domain.getObservationRule() != null) {
            entity.setThresholdPercent(domain.getObservationRule().getThresholdPercent());
            entity.setCheckFrequency(domain.getObservationRule().getCheckFrequency());
        }

        if (domain.getPollingInterval() != null) {
            entity.setPollingIntervalSeconds(domain.getPollingInterval().getSeconds());
        }

        entity.setLastPolledTimestamp(domain.getLastPolledTimestamp());
        entity.setActive(domain.isActive());

        return entity;
    }

    public static InventoryObserver toDomain(InventoryObserverEntity entity) {
        if (entity == null) {
            return null;
        }

        ObservationRule observationRule =
                new ObservationRule(entity.getThresholdPercent(), entity.getCheckFrequency());

        PollingInterval pollingInterval = new PollingInterval(entity.getPollingIntervalSeconds());

        InventoryObserver domain =
                new InventoryObserver(entity.getObserverId(), observationRule, pollingInterval);

        domain.setLastPolledTimestamp(entity.getLastPolledTimestamp());
        domain.setActive(entity.getActive());

        return domain;
    }
}
