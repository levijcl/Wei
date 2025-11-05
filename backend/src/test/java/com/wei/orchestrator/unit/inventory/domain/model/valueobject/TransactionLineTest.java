package com.wei.orchestrator.unit.inventory.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.inventory.domain.model.valueobject.TransactionLine;
import org.junit.jupiter.api.Test;

class TransactionLineTest {

    @Test
    void shouldCreateTransactionLineWithPositiveQuantity() {
        TransactionLine line = TransactionLine.of("SKU-001", 10);

        assertNotNull(line);
        assertEquals("SKU-001", line.getSku());
        assertEquals(10, line.getQuantity());
        assertTrue(line.isPositive());
        assertFalse(line.isNegative());
    }

    @Test
    void shouldCreateTransactionLineWithNegativeQuantity() {
        TransactionLine line = TransactionLine.of("SKU-001", -5);

        assertNotNull(line);
        assertEquals(-5, line.getQuantity());
        assertFalse(line.isPositive());
        assertTrue(line.isNegative());
    }

    @Test
    void shouldTrimSku() {
        TransactionLine line = TransactionLine.of("  SKU-001  ", 10);

        assertEquals("SKU-001", line.getSku());
    }

    @Test
    void shouldThrowExceptionWhenSkuIsNull() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> TransactionLine.of(null, 10));
        assertTrue(exception.getMessage().contains("SKU cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenSkuIsBlank() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> TransactionLine.of("   ", 10));
        assertTrue(exception.getMessage().contains("SKU cannot be null or blank"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsZero() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class, () -> TransactionLine.of("SKU-001", 0));
        assertTrue(exception.getMessage().contains("Quantity cannot be zero"));
    }

    @Test
    void shouldHaveValueEquality() {
        TransactionLine line1 = TransactionLine.of("SKU-001", 10);
        TransactionLine line2 = TransactionLine.of("SKU-001", 10);

        assertEquals(line1, line2);
        assertEquals(line1.hashCode(), line2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenSkuDiffers() {
        TransactionLine line1 = TransactionLine.of("SKU-001", 10);
        TransactionLine line2 = TransactionLine.of("SKU-002", 10);

        assertNotEquals(line1, line2);
    }

    @Test
    void shouldNotBeEqualWhenQuantityDiffers() {
        TransactionLine line1 = TransactionLine.of("SKU-001", 10);
        TransactionLine line2 = TransactionLine.of("SKU-001", 20);

        assertNotEquals(line1, line2);
    }

    @Test
    void shouldHaveReadableToString() {
        TransactionLine line = TransactionLine.of("SKU-001", 10);

        String result = line.toString();

        assertTrue(result.contains("SKU-001"));
        assertTrue(result.contains("10"));
    }
}
