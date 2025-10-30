package com.wei.orchestrator.unit.order.application.command;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateOrderCommandTest {

    @Test
    void shouldCreateCommandSuccessfully() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

        assertNotNull(command);
        assertEquals("ORDER-001", command.getOrderId());
        assertEquals(1, command.getItems().size());
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsNull() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand(null, items);
                        });

        assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsEmpty() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("", items);
                        });

        assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenOrderIdIsBlank() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("   ", items);
                        });

        assertTrue(exception.getMessage().contains("Order ID cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenItemsIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", null);
                        });

        assertTrue(exception.getMessage().contains("Order must have at least one item"));
    }

    @Test
    void shouldThrowExceptionWhenItemsIsEmpty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", new ArrayList<>());
                        });

        assertTrue(exception.getMessage().contains("Order must have at least one item"));
    }

    @Test
    void shouldThrowExceptionWhenSkuIsNull() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto(null, 10, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("SKU cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenSkuIsEmpty() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("", 10, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("SKU cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsNull() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(
                new CreateOrderCommand.OrderLineItemDto("SKU-001", null, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsZero() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 0, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void shouldThrowExceptionWhenQuantityIsNegative() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", -1, new BigDecimal("100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNull() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, null));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Price cannot be negative"));
    }

    @Test
    void shouldThrowExceptionWhenPriceIsNegative() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(
                new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("-100.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Price cannot be negative"));
    }

    @Test
    void shouldAllowZeroPrice() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, BigDecimal.ZERO));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

        assertNotNull(command);
        assertEquals(BigDecimal.ZERO, command.getItems().get(0).getPrice());
    }

    @Test
    void shouldCreateCommandWithMultipleItems() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-002", 5, new BigDecimal("50.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-003", 3, new BigDecimal("30.00")));

        CreateOrderCommand command = new CreateOrderCommand("ORDER-001", items);

        assertNotNull(command);
        assertEquals(3, command.getItems().size());
    }

    @Test
    void shouldThrowExceptionWhenOneOfMultipleItemsIsInvalid() {
        List<CreateOrderCommand.OrderLineItemDto> items = new ArrayList<>();
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-001", 10, new BigDecimal("100.00")));
        items.add(new CreateOrderCommand.OrderLineItemDto("SKU-002", -5, new BigDecimal("50.00")));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new CreateOrderCommand("ORDER-001", items);
                        });

        assertTrue(exception.getMessage().contains("Quantity must be greater than 0"));
    }
}
