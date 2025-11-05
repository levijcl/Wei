package com.wei.orchestrator.integration.inventory.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wei.orchestrator.inventory.domain.exception.InsufficientInventoryException;
import com.wei.orchestrator.inventory.domain.exception.InventorySystemException;
import com.wei.orchestrator.inventory.domain.exception.ReservationNotFoundException;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.infrastructure.adapter.InventoryHttpAdapter;
import com.wei.orchestrator.inventory.infrastructure.adapter.dto.CreateReservationResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ActiveProfiles("test")
@SpringBootTest
class InventoryHttpAdapterIntegrationTest {

    @Autowired private InventoryHttpAdapter inventoryHttpAdapter;

    @Autowired private RestTemplate restTemplate;

    @Value("${inventory.api.base-url}")
    private String inventoryApiBaseUrl;

    private MockRestServiceServer mockServer;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        objectMapper = new ObjectMapper();
    }

    @Nested
    class CreateReservationTests {

        @Test
        void shouldCreateReservationSuccessfully() throws Exception {
            CreateReservationResponse response = new CreateReservationResponse();
            response.setReservationId("RES-12345");
            String responseJson = objectMapper.writeValueAsString(response);

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.warehouse_id").value("WH-01"))
                    .andExpect(jsonPath("$.order_id").value("ORDER-001"))
                    .andExpect(jsonPath("$.quantity").value(10))
                    .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

            ExternalReservationId reservationId =
                    inventoryHttpAdapter.createReservation("SKU-001", "WH-01", "ORDER-001", 10);

            assertNotNull(reservationId);
            assertEquals("RES-12345", reservationId.getValue());
            mockServer.verify();
        }

        @Test
        void shouldThrowInsufficientInventoryExceptionOnConflict() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.CONFLICT));

            InsufficientInventoryException exception =
                    assertThrows(
                            InsufficientInventoryException.class,
                            () ->
                                    inventoryHttpAdapter.createReservation(
                                            "SKU-001", "WH-01", "ORDER-001", 100));

            assertTrue(exception.getMessage().contains("Insufficient inventory"));
            assertTrue(exception.getMessage().contains("SKU-001"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnBadRequest() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withBadRequest());

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.createReservation(
                                            "SKU-001", "WH-01", "ORDER-001", 10));

            assertTrue(exception.getMessage().contains("Invalid reservation request"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnServerError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withServerError());

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.createReservation(
                                            "SKU-001", "WH-01", "ORDER-001", 10));

            assertTrue(exception.getMessage().contains("Error creating reservation"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnNetworkError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new IOException("Network timeout")));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.createReservation(
                                            "SKU-001", "WH-01", "ORDER-001", 10));

            assertTrue(
                    exception.getMessage().contains("Error communicating with inventory system"));
            mockServer.verify();
        }
    }

    @Nested
    class ConsumeReservationTests {

        @Test
        void shouldConsumeReservationSuccessfully() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-12345");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-12345/consume"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess());

            assertDoesNotThrow(() -> inventoryHttpAdapter.consumeReservation(reservationId));
            mockServer.verify();
        }

        @Test
        void shouldThrowReservationNotFoundExceptionWhenReservationDoesNotExist() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-99999");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-99999/consume"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            ReservationNotFoundException exception =
                    assertThrows(
                            ReservationNotFoundException.class,
                            () -> inventoryHttpAdapter.consumeReservation(reservationId));

            assertTrue(exception.getMessage().contains("RES-99999"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionWhenReservationAlreadyConsumed() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-12345");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-12345/consume"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.CONFLICT));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () -> inventoryHttpAdapter.consumeReservation(reservationId));

            assertTrue(exception.getMessage().contains("already consumed"));
            assertTrue(exception.getMessage().contains("RES-12345"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnNetworkError() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-12345");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-12345/consume"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new IOException("Connection refused")));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () -> inventoryHttpAdapter.consumeReservation(reservationId));

            assertTrue(exception.getMessage().contains("Error consuming reservation"));
            mockServer.verify();
        }
    }

    @Nested
    class ReleaseReservationTests {

        @Test
        void shouldReleaseReservationSuccessfully() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-12345");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-12345/release"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess());

            assertDoesNotThrow(() -> inventoryHttpAdapter.releaseReservation(reservationId));
            mockServer.verify();
        }

        @Test
        void shouldThrowReservationNotFoundExceptionWhenReservationDoesNotExist() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-99999");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-99999/release"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            ReservationNotFoundException exception =
                    assertThrows(
                            ReservationNotFoundException.class,
                            () -> inventoryHttpAdapter.releaseReservation(reservationId));

            assertTrue(exception.getMessage().contains("RES-99999"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnNetworkError() {
            ExternalReservationId reservationId = ExternalReservationId.of("RES-12345");

            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/reservations/RES-12345/release"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new IOException("Timeout")));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () -> inventoryHttpAdapter.releaseReservation(reservationId));

            assertTrue(exception.getMessage().contains("Error releasing reservation"));
            mockServer.verify();
        }
    }

    @Nested
    class IncreaseInventoryTests {

        @Test
        void shouldIncreaseInventorySuccessfully() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/increase"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sku").value("SKU-001"))
                    .andExpect(jsonPath("$.warehouse_id").value("WH-01"))
                    .andExpect(jsonPath("$.quantity").value(50))
                    .andExpect(jsonPath("$.reason").value("Putaway completed"))
                    .andRespond(withSuccess());

            assertDoesNotThrow(
                    () ->
                            inventoryHttpAdapter.increaseInventory(
                                    "SKU-001", "WH-01", 50, "Putaway completed"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnClientError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/increase"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withBadRequest());

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.increaseInventory(
                                            "SKU-001", "WH-01", 50, "Putaway completed"));

            assertTrue(exception.getMessage().contains("Error increasing inventory"));
            assertTrue(exception.getMessage().contains("SKU-001"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnNetworkError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/increase"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new IOException("Connection timeout")));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.increaseInventory(
                                            "SKU-001", "WH-01", 50, "Putaway completed"));

            assertTrue(
                    exception.getMessage().contains("Error communicating with inventory system"));
            mockServer.verify();
        }
    }

    @Nested
    class AdjustInventoryTests {

        @Test
        void shouldAdjustInventorySuccessfully() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/adjust"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sku").value("SKU-002"))
                    .andExpect(jsonPath("$.warehouse_id").value("WH-01"))
                    .andExpect(jsonPath("$.adjustment").value(-5))
                    .andExpect(jsonPath("$.reason").value("Damaged goods"))
                    .andRespond(withSuccess());

            assertDoesNotThrow(
                    () ->
                            inventoryHttpAdapter.adjustInventory(
                                    "SKU-002", "WH-01", -5, "Damaged goods"));
            mockServer.verify();
        }

        @Test
        void shouldAdjustInventoryWithPositiveChange() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/adjust"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sku").value("SKU-003"))
                    .andExpect(jsonPath("$.warehouse_id").value("WH-02"))
                    .andExpect(jsonPath("$.adjustment").value(10))
                    .andExpect(jsonPath("$.reason").value("Found during cycle count"))
                    .andRespond(withSuccess());

            assertDoesNotThrow(
                    () ->
                            inventoryHttpAdapter.adjustInventory(
                                    "SKU-003", "WH-02", 10, "Found during cycle count"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnClientError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/adjust"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withBadRequest());

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.adjustInventory(
                                            "SKU-002", "WH-01", -5, "Damaged goods"));

            assertTrue(exception.getMessage().contains("Error adjusting inventory"));
            assertTrue(exception.getMessage().contains("SKU-002"));
            mockServer.verify();
        }

        @Test
        void shouldThrowInventorySystemExceptionOnNetworkError() {
            mockServer
                    .expect(requestTo(inventoryApiBaseUrl + "/api/inventory/adjust"))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withException(new IOException("Service unavailable")));

            InventorySystemException exception =
                    assertThrows(
                            InventorySystemException.class,
                            () ->
                                    inventoryHttpAdapter.adjustInventory(
                                            "SKU-002", "WH-01", -5, "Damaged goods"));

            assertTrue(
                    exception.getMessage().contains("Error communicating with inventory system"));
            mockServer.verify();
        }
    }
}
