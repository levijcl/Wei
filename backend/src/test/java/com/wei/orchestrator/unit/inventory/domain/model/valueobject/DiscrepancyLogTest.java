package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.DiscrepancyLog;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiscrepancyLogTest {

    @Nested
    class OfMethodTest {
        @Test
        void shouldCreateDiscrepancyLogWithPositiveDifference() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 120);

            assertNotNull(log);
            assertEquals("SKU-001", log.getSku());
            assertEquals("WH-01", log.getWarehouseId());
            assertEquals(100, log.getExpectedQuantity());
            assertEquals(120, log.getActualQuantity());
            assertEquals(20, log.getDifference());
            assertNotNull(log.getDetectedAt());
        }

        @Test
        void shouldCreateDiscrepancyLogWithNegativeDifference() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-002", "WH-01", 100, 75);

            assertEquals(-25, log.getDifference());
        }

        @Test
        void shouldCreateDiscrepancyLogWithZeroDifference() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-003", "WH-01", 50, 50);

            assertEquals(0, log.getDifference());
        }

        @Test
        void shouldCreateDiscrepancyLogWithExpectedZero() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-004", "WH-01", 0, 30);

            assertEquals(0, log.getExpectedQuantity());
            assertEquals(30, log.getActualQuantity());
            assertEquals(30, log.getDifference());
        }

        @Test
        void shouldCreateDiscrepancyLogWithActualZero() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-005", "WH-01", 50, 0);

            assertEquals(50, log.getExpectedQuantity());
            assertEquals(0, log.getActualQuantity());
            assertEquals(-50, log.getDifference());
        }

        @Test
        void shouldTrimWhitespaceFromSku() {
            DiscrepancyLog log = DiscrepancyLog.of("  SKU-006  ", "WH-01", 10, 15);

            assertEquals("SKU-006", log.getSku());
        }

        @Test
        void shouldTrimWhitespaceFromWarehouseId() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-007", "  WH-02  ", 10, 15);

            assertEquals("WH-02", log.getWarehouseId());
        }

        @Test
        void shouldThrowExceptionWhenSkuIsNull() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of(null, "WH-01", 10, 15));
            assertTrue(exception.getMessage().contains("SKU cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenSkuIsBlank() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of("   ", "WH-01", 10, 15));
            assertTrue(exception.getMessage().contains("SKU cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenWarehouseIdIsNull() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of("SKU-001", null, 10, 15));
            assertTrue(exception.getMessage().contains("Warehouse ID cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenWarehouseIdIsBlank() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of("SKU-001", "   ", 10, 15));
            assertTrue(exception.getMessage().contains("Warehouse ID cannot be null or blank"));
        }

        @Test
        void shouldThrowExceptionWhenExpectedQuantityIsNegative() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of("SKU-001", "WH-01", -5, 10));
            assertTrue(exception.getMessage().contains("Expected quantity cannot be negative"));
        }

        @Test
        void shouldThrowExceptionWhenActualQuantityIsNegative() {
            IllegalArgumentException exception =
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> DiscrepancyLog.of("SKU-001", "WH-01", 10, -3));
            assertTrue(exception.getMessage().contains("Actual quantity cannot be negative"));
        }
    }

    @Nested
    class HasDiscrepancyMethodTest {
        @Test
        void shouldReturnTrueWhenDifferenceIsPositive() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 110);

            assertTrue(log.hasDiscrepancy());
        }

        @Test
        void shouldReturnTrueWhenDifferenceIsNegative() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 90);

            assertTrue(log.hasDiscrepancy());
        }

        @Test
        void shouldReturnFalseWhenDifferenceIsZero() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 100);

            assertFalse(log.hasDiscrepancy());
        }
    }

    @Nested
    class IsOverstockMethodTest {
        @Test
        void shouldReturnTrueWhenActualIsGreaterThanExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 120);

            assertTrue(log.isOverstock());
        }

        @Test
        void shouldReturnFalseWhenActualIsLessThanExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 80);

            assertFalse(log.isOverstock());
        }

        @Test
        void shouldReturnFalseWhenActualEqualsExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 100);

            assertFalse(log.isOverstock());
        }
    }

    @Nested
    class IsUnderstockMethodTest {
        @Test
        void shouldReturnTrueWhenActualIsLessThanExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 75);

            assertTrue(log.isUnderstock());
        }

        @Test
        void shouldReturnFalseWhenActualIsGreaterThanExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 125);

            assertFalse(log.isUnderstock());
        }

        @Test
        void shouldReturnFalseWhenActualEqualsExpected() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 100);

            assertFalse(log.isUnderstock());
        }
    }

    @Nested
    class EqualsAndHashCodeTest {
        @Test
        void shouldNotBeEqualWhenSkuDiffers() {
            DiscrepancyLog log1 = DiscrepancyLog.of("SKU-001", "WH-01", 100, 110);
            DiscrepancyLog log2 = DiscrepancyLog.of("SKU-002", "WH-01", 100, 110);

            assertNotEquals(log1, log2);
        }

        @Test
        void shouldNotBeEqualWhenWarehouseIdDiffers() {
            DiscrepancyLog log1 = DiscrepancyLog.of("SKU-001", "WH-01", 100, 110);
            DiscrepancyLog log2 = DiscrepancyLog.of("SKU-001", "WH-02", 100, 110);

            assertNotEquals(log1, log2);
        }

        @Test
        void shouldNotBeEqualWhenExpectedQuantityDiffers() {
            DiscrepancyLog log1 = DiscrepancyLog.of("SKU-001", "WH-01", 100, 110);
            DiscrepancyLog log2 = DiscrepancyLog.of("SKU-001", "WH-01", 90, 110);

            assertNotEquals(log1, log2);
        }

        @Test
        void shouldNotBeEqualWhenActualQuantityDiffers() {
            DiscrepancyLog log1 = DiscrepancyLog.of("SKU-001", "WH-01", 100, 110);
            DiscrepancyLog log2 = DiscrepancyLog.of("SKU-001", "WH-01", 100, 120);

            assertNotEquals(log1, log2);
        }
    }

    @Nested
    class ToStringTest {
        @Test
        void shouldReturnFormattedString() {
            DiscrepancyLog log = DiscrepancyLog.of("SKU-001", "WH-01", 100, 120);

            String result = log.toString();

            assertTrue(result.contains("SKU-001"));
            assertTrue(result.contains("WH-01"));
            assertTrue(result.contains("100"));
            assertTrue(result.contains("120"));
            assertTrue(result.contains("20"));
        }
    }
}
