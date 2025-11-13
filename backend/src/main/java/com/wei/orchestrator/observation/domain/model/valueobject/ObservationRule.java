package com.wei.orchestrator.observation.domain.model.valueobject;

import java.util.Objects;

public class ObservationRule {
    private final double thresholdPercent;
    private final int checkFrequency;

    public ObservationRule(double thresholdPercent, int checkFrequency) {
        if (thresholdPercent < 0 || thresholdPercent > 100) {
            throw new IllegalArgumentException("Threshold percent must be between 0 and 100");
        }
        if (checkFrequency <= 0) {
            throw new IllegalArgumentException("Check frequency must be positive");
        }
        this.thresholdPercent = thresholdPercent;
        this.checkFrequency = checkFrequency;
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    public int getCheckFrequency() {
        return checkFrequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservationRule that = (ObservationRule) o;
        return Double.compare(that.thresholdPercent, thresholdPercent) == 0
                && checkFrequency == that.checkFrequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(thresholdPercent, checkFrequency);
    }

    @Override
    public String toString() {
        return "ObservationRule{"
                + "thresholdPercent="
                + thresholdPercent
                + ", checkFrequency="
                + checkFrequency
                + '}';
    }
}
