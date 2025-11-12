package com.wei.orchestrator.observation.domain.event;

import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesTaskDto;
import java.time.LocalDateTime;

public class WesTaskDiscoveredEvent {
    private final WesTaskDto wesTaskDto;
    private final LocalDateTime occurredAt;

    public WesTaskDiscoveredEvent(WesTaskDto wesTaskDto) {
        this.wesTaskDto = wesTaskDto;
        this.occurredAt = LocalDateTime.now();
    }

    public WesTaskDto getWesTaskDto() {
        return wesTaskDto;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "WesTaskDiscoveredEvent{"
                + "wesTaskDto="
                + wesTaskDto
                + ", occurredAt="
                + occurredAt
                + '}';
    }
}
