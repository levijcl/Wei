package com.wei.orchestrator.wes.domain.model.valueobject;

import java.util.Objects;

public final class WesTaskId {
    private final String value;

    private WesTaskId(String value) {
        this.value = value;
    }

    public static WesTaskId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("WES task ID cannot be null or blank");
        }
        return new WesTaskId(value.trim());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WesTaskId wesTaskId = (WesTaskId) o;
        return Objects.equals(value, wesTaskId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "WesTaskId{" + value + "}";
    }
}
