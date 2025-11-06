package com.wei.orchestrator.order.domain.model.valueobject;

import java.time.Duration;
import java.util.Objects;

public class FulfillmentLeadTime {
    private static final Duration DEFAULT_LEAD_TIME = Duration.ofHours(2);
    private final Duration duration;

    public FulfillmentLeadTime(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("Duration cannot be null");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
        this.duration = duration;
    }

    public static FulfillmentLeadTime defaultLeadTime() {
        return new FulfillmentLeadTime(DEFAULT_LEAD_TIME);
    }

    public static FulfillmentLeadTime ofHours(long hours) {
        return new FulfillmentLeadTime(Duration.ofHours(hours));
    }

    public static FulfillmentLeadTime ofMinutes(long minutes) {
        return new FulfillmentLeadTime(Duration.ofMinutes(minutes));
    }

    public Duration getDuration() {
        return duration;
    }

    public long getMinutes() {
        return duration.toMinutes();
    }

    public long getHours() {
        return duration.toHours();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FulfillmentLeadTime that = (FulfillmentLeadTime) o;
        return Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration);
    }

    @Override
    public String toString() {
        return "FulfillmentLeadTime{" + "duration=" + duration + '}';
    }
}
