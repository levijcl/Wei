package com.wei.orchestrator.observation.infrastructure.mapper;

import com.wei.orchestrator.observation.domain.model.WesObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.TaskEndpoint;
import com.wei.orchestrator.observation.infrastructure.persistence.WesObserverEntity;

public class WesObserverMapper {

    public static WesObserverEntity toEntity(WesObserver domain) {
        if (domain == null) {
            return null;
        }

        WesObserverEntity entity = new WesObserverEntity();
        entity.setObserverId(domain.getObserverId());

        if (domain.getTaskEndpoint() != null) {
            entity.setTaskEndpointUrl(domain.getTaskEndpoint().getUrl());
            entity.setTaskEndpointAuthToken(domain.getTaskEndpoint().getAuthToken());
        }

        if (domain.getPollingInterval() != null) {
            entity.setPollingIntervalSeconds(domain.getPollingInterval().getSeconds());
        }

        entity.setLastPolledTimestamp(domain.getLastPolledTimestamp());
        entity.setActive(domain.isActive());

        return entity;
    }

    public static WesObserver toDomain(WesObserverEntity entity) {
        if (entity == null) {
            return null;
        }

        TaskEndpoint taskEndpoint =
                new TaskEndpoint(entity.getTaskEndpointUrl(), entity.getTaskEndpointAuthToken());

        PollingInterval pollingInterval = new PollingInterval(entity.getPollingIntervalSeconds());

        WesObserver domain = new WesObserver(entity.getObserverId(), taskEndpoint, pollingInterval);

        domain.setLastPolledTimestamp(entity.getLastPolledTimestamp());
        domain.setActive(entity.getActive());

        return domain;
    }
}
