package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.WarehouseLocation;
import org.junit.jupiter.api.Test;

class WarehouseLocationTest {

    @Test
    void shouldCreateWarehouseLocationWithWarehouseIdAndZone() {
        WarehouseLocation location = WarehouseLocation.of("WH-01", "ZONE-A");

        assertNotNull(location);
        assertEquals("WH-01", location.getWarehouseId());
        assertEquals("ZONE-A", location.getZone());
        assertTrue(location.hasZone());
    }

    @Test
    void shouldCreateWarehouseLocationWithOnlyWarehouseId() {
        WarehouseLocation location = WarehouseLocation.of("WH-01");

        assertNotNull(location);
        assertEquals("WH-01", location.getWarehouseId());
        assertNull(location.getZone());
        assertFalse(location.hasZone());
    }

    @Test
    void shouldTrimWarehouseIdAndZone() {
        WarehouseLocation location = WarehouseLocation.of("  WH-01  ", "  ZONE-A  ");

        assertEquals("WH-01", location.getWarehouseId());
        assertEquals("ZONE-A", location.getZone());
    }

    @Test
    void shouldThrowExceptionWhenWarehouseIdIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> WarehouseLocation.of(null, "ZONE-A"));
        assertTrue(exception.getMessage().contains("Warehouse ID cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenWarehouseIdIsBlank() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WarehouseLocation.of("   ", "ZONE-A"));
        assertTrue(exception.getMessage().contains("Warehouse ID cannot be null or blank"));
    }

    @Test
    void shouldAllowNullZone() {
        WarehouseLocation location = WarehouseLocation.of("WH-01", null);

        assertEquals("WH-01", location.getWarehouseId());
        assertNull(location.getZone());
        assertFalse(location.hasZone());
    }

    @Test
    void shouldNotHaveZoneWhenZoneIsBlank() {
        WarehouseLocation location = WarehouseLocation.of("WH-01", "   ");

        assertFalse(location.hasZone());
    }

    @Test
    void shouldHaveValueEquality() {
        WarehouseLocation location1 = WarehouseLocation.of("WH-01", "ZONE-A");
        WarehouseLocation location2 = WarehouseLocation.of("WH-01", "ZONE-A");

        assertEquals(location1, location2);
        assertEquals(location1.hashCode(), location2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenWarehouseIdDiffers() {
        WarehouseLocation location1 = WarehouseLocation.of("WH-01", "ZONE-A");
        WarehouseLocation location2 = WarehouseLocation.of("WH-02", "ZONE-A");

        assertNotEquals(location1, location2);
    }

    @Test
    void shouldNotBeEqualWhenZoneDiffers() {
        WarehouseLocation location1 = WarehouseLocation.of("WH-01", "ZONE-A");
        WarehouseLocation location2 = WarehouseLocation.of("WH-01", "ZONE-B");

        assertNotEquals(location1, location2);
    }

    @Test
    void shouldHaveReadableToStringWithZone() {
        WarehouseLocation location = WarehouseLocation.of("WH-01", "ZONE-A");

        String result = location.toString();

        assertTrue(result.contains("WH-01"));
        assertTrue(result.contains("ZONE-A"));
    }

    @Test
    void shouldHaveReadableToStringWithoutZone() {
        WarehouseLocation location = WarehouseLocation.of("WH-01");

        String result = location.toString();

        assertTrue(result.contains("WH-01"));
        assertFalse(result.contains("zone"));
    }
}
