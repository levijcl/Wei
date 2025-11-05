package com.wei.orchestrator.inventory.domain.model.valueobject;

import java.util.Objects;

public final class ExternalReservationId {
    private final String value;

    private ExternalReservationId(String value) {
        this.value = value;
    }

    public static ExternalReservationId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("External reservation ID cannot be null or blank");
        }
        return new ExternalReservationId(value.trim());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExternalReservationId that = (ExternalReservationId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
