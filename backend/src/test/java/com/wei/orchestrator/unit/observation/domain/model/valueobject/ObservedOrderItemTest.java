package com.wei.orchestrator.unit.observation.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ObservedOrderItemTest {

    @Test
    void shouldCreateObservedOrderItemWithValidParameters() {
        ObservedOrderItem item =
                new ObservedOrderItem("SKU-001", "Product Name", 5, BigDecimal.valueOf(100.50));

        assertNotNull(item);
        assertEquals("SKU-001", item.getSku());
        assertEquals("Product Name", item.getProductName());
        assertEquals(5, item.getQuantity());
        assertEquals(BigDecimal.valueOf(100.50), item.getPrice());
    }

    @Test
    void shouldThrowExceptionWhenSkuIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem(null, "Product", 1, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("SKU cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenSkuIsEmpty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("", "Product", 1, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("SKU cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenProductNameIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", null, 1, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("Product name cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenProductNameIsEmpty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", "", 1, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("Product name cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsZero() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", "Product", 0, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be positive"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", "Product", -1, BigDecimal.TEN);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be positive"));
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", "Product", 1, null);
                        });

        assertTrue(exception.getMessage().contains("Price cannot be null or negative"));
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ObservedOrderItem("SKU-001", "Product", 1, BigDecimal.valueOf(-10));
                        });

        assertTrue(exception.getMessage().contains("Price cannot be null or negative"));
    }

    @Test
    void shouldAcceptZeroPrice() {
        ObservedOrderItem item =
                new ObservedOrderItem("SKU-001", "Free Product", 1, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, item.getPrice());
    }

    @Test
    void shouldBeEqualWhenAllFieldsAreSame() {
        ObservedOrderItem item1 =
                new ObservedOrderItem("SKU-001", "Product", 5, BigDecimal.valueOf(100.50));
        ObservedOrderItem item2 =
                new ObservedOrderItem("SKU-001", "Product", 5, BigDecimal.valueOf(100.50));

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenSkuIsDifferent() {
        ObservedOrderItem item1 =
                new ObservedOrderItem("SKU-001", "Product", 5, BigDecimal.valueOf(100.50));
        ObservedOrderItem item2 =
                new ObservedOrderItem("SKU-002", "Product", 5, BigDecimal.valueOf(100.50));

        assertNotEquals(item1, item2);
    }

    @Test
    void shouldNotBeEqualWhenQuantityIsDifferent() {
        ObservedOrderItem item1 =
                new ObservedOrderItem("SKU-001", "Product", 5, BigDecimal.valueOf(100.50));
        ObservedOrderItem item2 =
                new ObservedOrderItem("SKU-001", "Product", 10, BigDecimal.valueOf(100.50));

        assertNotEquals(item1, item2);
    }
}
