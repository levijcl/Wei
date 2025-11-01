package com.wei.orchestrator.observation.infrastructure.mapper;

import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.infrastructure.persistence.OrderObserverEntity;

public class OrderObserverMapper {

    public static OrderObserverEntity toEntity(OrderObserver domain) {
        if (domain == null) {
            return null;
        }

        OrderObserverEntity entity = new OrderObserverEntity();
        entity.setObserverId(domain.getObserverId());

        if (domain.getSourceEndpoint() != null) {
            entity.setJdbcUrl(domain.getSourceEndpoint().getJdbcUrl());
            entity.setUsername(domain.getSourceEndpoint().getUsername());
            entity.setPassword(domain.getSourceEndpoint().getPassword());
        }

        if (domain.getPollingInterval() != null) {
            entity.setPollingIntervalSeconds(domain.getPollingInterval().getSeconds());
        }

        entity.setLastPolledTimestamp(domain.getLastPolledTimestamp());
        entity.setActive(domain.isActive());

        return entity;
    }

    public static OrderObserver toDomain(OrderObserverEntity entity) {
        if (entity == null) {
            return null;
        }

        SourceEndpoint sourceEndpoint =
                new SourceEndpoint(entity.getJdbcUrl(), entity.getUsername(), entity.getPassword());

        PollingInterval pollingInterval = new PollingInterval(entity.getPollingIntervalSeconds());

        OrderObserver domain =
                new OrderObserver(entity.getObserverId(), sourceEndpoint, pollingInterval);

        domain.setLastPolledTimestamp(entity.getLastPolledTimestamp());
        domain.setActive(entity.getActive());

        return domain;
    }
}
