package com.wei.orchestrator.observation.domain.model.valueobject;

import java.util.Objects;

public class PollingInterval {
    private final int seconds;

    public PollingInterval(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Polling interval must be positive");
        }
        if (seconds < 10) {
            throw new IllegalArgumentException("Polling interval must be at least 10 seconds");
        }
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    public long getMilliseconds() {
        return seconds * 1000L;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PollingInterval that = (PollingInterval) o;
        return seconds == that.seconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds);
    }

    @Override
    public String toString() {
        return "PollingInterval{" + "seconds=" + seconds + '}';
    }
}
