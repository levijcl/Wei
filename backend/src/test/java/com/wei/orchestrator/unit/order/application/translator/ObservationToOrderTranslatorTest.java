package com.wei.orchestrator.unit.order.application.translator;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.order.application.command.CreateOrderCommand;
import com.wei.orchestrator.order.application.command.CreateOrderCommand.OrderLineItemDto;
import com.wei.orchestrator.order.application.translator.ObservationToOrderTranslator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ObservationToOrderTranslatorTest {

    private ObservationToOrderTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new ObservationToOrderTranslator();
    }

    @Nested
    class TranslateMethodTests {

        @Test
        void shouldTranslateSingleItemObservationResult() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 5, BigDecimal.valueOf(10.99)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-001",
                            "John Doe",
                            "john@example.com",
                            "123 Main St",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertNotNull(command);
            assertEquals("ORDER-001", command.getOrderId());
            assertNotNull(command.getItems());
            assertEquals(1, command.getItems().size());

            OrderLineItemDto itemDto = command.getItems().get(0);
            assertEquals("SKU-001", itemDto.getSku());
            assertEquals(5, itemDto.getQuantity());
            assertEquals(BigDecimal.valueOf(10.99), itemDto.getPrice());
        }

        @Test
        void shouldTranslateMultipleItemsObservationResult() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 5, BigDecimal.valueOf(10.99)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-002", "Product SKU-002", 3, BigDecimal.valueOf(25.50)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-003", "Product SKU-003", 10, BigDecimal.valueOf(5.00)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-002",
                            "Jane Smith",
                            "jane@example.com",
                            "456 Oak Ave",
                            "EXPRESS",
                            "WH-02",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertNotNull(command);
            assertEquals("ORDER-002", command.getOrderId());
            assertEquals(3, command.getItems().size());

            OrderLineItemDto item1 = command.getItems().get(0);
            assertEquals("SKU-001", item1.getSku());
            assertEquals(5, item1.getQuantity());
            assertEquals(BigDecimal.valueOf(10.99), item1.getPrice());

            OrderLineItemDto item2 = command.getItems().get(1);
            assertEquals("SKU-002", item2.getSku());
            assertEquals(3, item2.getQuantity());
            assertEquals(BigDecimal.valueOf(25.50), item2.getPrice());

            OrderLineItemDto item3 = command.getItems().get(2);
            assertEquals("SKU-003", item3.getSku());
            assertEquals(10, item3.getQuantity());
            assertEquals(BigDecimal.valueOf(5.00), item3.getPrice());
        }

        @Test
        void shouldTranslateWithDifferentPriceFormats() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 1, BigDecimal.valueOf(99.99)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-002", "Product SKU-002", 2, BigDecimal.valueOf(0.99)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-003", "Product SKU-003", 3, BigDecimal.valueOf(1000.00)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-003",
                            "Bob Johnson",
                            "bob@example.com",
                            "789 Pine Rd",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertNotNull(command);
            assertEquals(3, command.getItems().size());

            assertEquals(BigDecimal.valueOf(99.99), command.getItems().get(0).getPrice());
            assertEquals(BigDecimal.valueOf(0.99), command.getItems().get(1).getPrice());
            assertEquals(BigDecimal.valueOf(1000.00), command.getItems().get(2).getPrice());
        }

        @Test
        void shouldTranslateWithVaryingQuantities() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 1, BigDecimal.valueOf(10.00)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-002", "Product SKU-002", 100, BigDecimal.valueOf(1.00)));
            items.add(
                    new ObservedOrderItem(
                            "SKU-003", "Product SKU-003", 50, BigDecimal.valueOf(2.50)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-004",
                            "Alice Brown",
                            "alice@example.com",
                            "321 Elm St",
                            "BULK",
                            "WH-03",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertNotNull(command);
            assertEquals(3, command.getItems().size());

            assertEquals(1, command.getItems().get(0).getQuantity());
            assertEquals(100, command.getItems().get(1).getQuantity());
            assertEquals(50, command.getItems().get(2).getQuantity());
        }

        @Test
        void shouldPreserveOrderIdDuringTranslation() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 5, BigDecimal.valueOf(10.00)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-12345-ABCDE",
                            "Test User",
                            "test@example.com",
                            "Test Address",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertEquals("ORDER-12345-ABCDE", command.getOrderId());
        }

        @Test
        void shouldThrowExceptionWhenObservationResultIsNull() {
            IllegalArgumentException exception =
                    assertThrows(IllegalArgumentException.class, () -> translator.translate(null));

            assertEquals("ObservationResult cannot be null", exception.getMessage());
        }

        @Test
        void shouldHandleItemsInOrder() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(new ObservedOrderItem("SKU-A", "Product SKU-A", 1, BigDecimal.valueOf(1.00)));
            items.add(new ObservedOrderItem("SKU-B", "Product SKU-B", 2, BigDecimal.valueOf(2.00)));
            items.add(new ObservedOrderItem("SKU-C", "Product SKU-C", 3, BigDecimal.valueOf(3.00)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-005",
                            "Test User",
                            "test@example.com",
                            "Test Address",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertEquals("SKU-A", command.getItems().get(0).getSku());
            assertEquals("SKU-B", command.getItems().get(1).getSku());
            assertEquals("SKU-C", command.getItems().get(2).getSku());
        }
    }

    @Nested
    class IsolationTests {

        @Test
        void shouldNotExposeObservationResultFieldsInCommand() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 5, BigDecimal.valueOf(10.00)));

            ObservationResult observationResult =
                    new ObservationResult(
                            "ORDER-001",
                            "John Doe",
                            "john@example.com",
                            "123 Main St",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command = translator.translate(observationResult);

            assertNotNull(command.getOrderId());
            assertNotNull(command.getItems());
        }

        @Test
        void shouldTranslateIndependentlyOfObservationResultChanges() {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-001", "Product SKU-001", 5, BigDecimal.valueOf(10.00)));

            ObservationResult observationResult1 =
                    new ObservationResult(
                            "ORDER-001",
                            "Customer A",
                            "a@example.com",
                            "Address A",
                            "STANDARD",
                            "WH-01",
                            "NEW",
                            null,
                            items,
                            LocalDateTime.now());

            ObservationResult observationResult2 =
                    new ObservationResult(
                            "ORDER-002",
                            "Customer B",
                            "b@example.com",
                            "Address B",
                            "EXPRESS",
                            "WH-02",
                            "PROCESSING",
                            null,
                            items,
                            LocalDateTime.now());

            CreateOrderCommand command1 = translator.translate(observationResult1);
            CreateOrderCommand command2 = translator.translate(observationResult2);

            assertEquals("ORDER-001", command1.getOrderId());
            assertEquals("ORDER-002", command2.getOrderId());

            assertNotEquals(command1.getOrderId(), command2.getOrderId());
        }
    }
}
