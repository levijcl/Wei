package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import org.junit.jupiter.api.Test;

class ExternalReservationIdTest {

    @Test
    void shouldCreateExternalReservationId() {
        ExternalReservationId id = ExternalReservationId.of("EXT-RES-001");

        assertNotNull(id);
        assertEquals("EXT-RES-001", id.getValue());
    }

    @Test
    void shouldTrimValue() {
        ExternalReservationId id = ExternalReservationId.of("  EXT-RES-001  ");

        assertEquals("EXT-RES-001", id.getValue());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ExternalReservationId.of(null));
        assertTrue(
                exception.getMessage().contains("External reservation ID cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenValueIsBlank() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> ExternalReservationId.of("   "));
        assertTrue(
                exception.getMessage().contains("External reservation ID cannot be null or blank"));
    }

    @Test
    void shouldHaveValueEquality() {
        ExternalReservationId id1 = ExternalReservationId.of("EXT-RES-001");
        ExternalReservationId id2 = ExternalReservationId.of("EXT-RES-001");

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenValueDiffers() {
        ExternalReservationId id1 = ExternalReservationId.of("EXT-RES-001");
        ExternalReservationId id2 = ExternalReservationId.of("EXT-RES-002");

        assertNotEquals(id1, id2);
    }

    @Test
    void shouldHaveReadableToString() {
        ExternalReservationId id = ExternalReservationId.of("EXT-RES-001");

        String result = id.toString();

        assertEquals("EXT-RES-001", result);
    }
}
