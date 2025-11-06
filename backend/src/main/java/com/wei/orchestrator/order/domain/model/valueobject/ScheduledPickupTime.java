package com.wei.orchestrator.order.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

public class ScheduledPickupTime {
    private final LocalDateTime pickupTime;

    public ScheduledPickupTime(LocalDateTime pickupTime) {
        if (pickupTime == null) {
            throw new IllegalArgumentException("Pickup time cannot be null");
        }
        this.pickupTime = pickupTime;
    }

    public boolean isInFuture(LocalDateTime currentTime) {
        return pickupTime.isAfter(currentTime);
    }

    public LocalDateTime calculateFulfillmentStartTime(FulfillmentLeadTime leadTime) {
        if (leadTime == null) {
            throw new IllegalArgumentException("Lead time cannot be null");
        }
        return pickupTime.minus(leadTime.getDuration());
    }

    public LocalDateTime getPickupTime() {
        return pickupTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledPickupTime that = (ScheduledPickupTime) o;
        return Objects.equals(pickupTime, that.pickupTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pickupTime);
    }

    @Override
    public String toString() {
        return "ScheduledPickupTime{" + "pickupTime=" + pickupTime + '}';
    }
}
