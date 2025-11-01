package com.wei.orchestrator.observation.domain.port;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderSourcePort {

    List<ObservationResult> fetchNewOrders(SourceEndpoint sourceEndpoint, LocalDateTime since);

    boolean markOrderAsProcessed(SourceEndpoint sourceEndpoint, String orderId);
}
