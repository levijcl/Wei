package com.wei.orchestrator.inventory.infrastructure.adapter;

import com.wei.orchestrator.inventory.domain.exception.InsufficientInventoryException;
import com.wei.orchestrator.inventory.domain.exception.InventorySystemException;
import com.wei.orchestrator.inventory.domain.exception.ReservationNotFoundException;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;
import com.wei.orchestrator.inventory.domain.port.InventoryPort;
import com.wei.orchestrator.inventory.infrastructure.adapter.dto.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class InventoryHttpAdapter implements InventoryPort {

    private final RestTemplate restTemplate;
    private final String inventoryApiBaseUrl;

    public InventoryHttpAdapter(
            RestTemplate restTemplate,
            @Value("${inventory.api.base-url}") String inventoryApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.inventoryApiBaseUrl = inventoryApiBaseUrl;
    }

    @Override
    public ExternalReservationId createReservation(
            String sku, String warehouseId, String orderId, int quantity)
            throws InsufficientInventoryException, InventorySystemException {

        CreateReservationRequest request =
                new CreateReservationRequest(sku, warehouseId, orderId, quantity);

        try {
            String url = inventoryApiBaseUrl + "/api/reservations";
            ResponseEntity<CreateReservationResponse> response =
                    restTemplate.postForEntity(url, request, CreateReservationResponse.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getData() != null) {
                return ExternalReservationId.of(response.getBody().getData().getReservationId());
            }

            throw new InventorySystemException(
                    "Failed to create reservation: " + response.getStatusCode());

        } catch (HttpClientErrorException.Conflict e) {
            throw new InsufficientInventoryException(
                    "Insufficient inventory for SKU: " + sku + " in warehouse: " + warehouseId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new InventorySystemException(
                        "Invalid reservation request: " + e.getMessage(), e);
            }
            throw new InventorySystemException(
                    "Error creating reservation: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new InventorySystemException(
                    "Error creating reservation: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new InventorySystemException("Error communicating with inventory system", e);
        }
    }

    @Override
    public void consumeReservation(ExternalReservationId reservationId)
            throws ReservationNotFoundException, InventorySystemException {

        try {
            String url =
                    inventoryApiBaseUrl
                            + "/api/reservations/"
                            + reservationId.getValue()
                            + "/consume";
            restTemplate.postForEntity(url, null, Void.class);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ReservationNotFoundException(reservationId.getValue());
        } catch (HttpClientErrorException.Conflict e) {
            throw new InventorySystemException(
                    "Reservation already consumed or in invalid state: " + reservationId.getValue(),
                    e);
        } catch (RestClientException e) {
            throw new InventorySystemException(
                    "Error consuming reservation: " + reservationId.getValue(), e);
        }
    }

    @Override
    public void releaseReservation(ExternalReservationId reservationId)
            throws ReservationNotFoundException, InventorySystemException {

        try {
            String url =
                    inventoryApiBaseUrl
                            + "/api/reservations/"
                            + reservationId.getValue()
                            + "/release";
            restTemplate.postForEntity(url, null, Void.class);

        } catch (HttpClientErrorException.NotFound e) {
            throw new ReservationNotFoundException(reservationId.getValue());
        } catch (RestClientException e) {
            throw new InventorySystemException(
                    "Error releasing reservation: " + reservationId.getValue(), e);
        }
    }

    @Override
    public void increaseInventory(String sku, String warehouseId, int quantity, String reason)
            throws InventorySystemException {

        IncreaseInventoryRequest request =
                new IncreaseInventoryRequest(sku, warehouseId, quantity, reason);

        try {
            String url = inventoryApiBaseUrl + "/api/inventory/increase";
            restTemplate.postForEntity(url, request, Void.class);

        } catch (HttpClientErrorException e) {
            throw new InventorySystemException(
                    "Error increasing inventory for SKU: " + sku + " - " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new InventorySystemException("Error communicating with inventory system", e);
        }
    }

    @Override
    public void adjustInventory(String sku, String warehouseId, int quantityChange, String reason)
            throws InventorySystemException {

        AdjustInventoryRequest request =
                new AdjustInventoryRequest(sku, warehouseId, quantityChange, reason);

        try {
            String url = inventoryApiBaseUrl + "/api/inventory/adjust";
            restTemplate.postForEntity(url, request, Void.class);

        } catch (HttpClientErrorException e) {
            throw new InventorySystemException(
                    "Error adjusting inventory for SKU: " + sku + " - " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new InventorySystemException("Error communicating with inventory system", e);
        }
    }

    @Override
    public List<InventorySnapshotDto> getInventorySnapshot() throws InventorySystemException {
        try {
            String url = inventoryApiBaseUrl + "/api/inventory";
            ResponseEntity<InventorySnapshotResponse> response =
                    restTemplate.getForEntity(url, InventorySnapshotResponse.class);

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getSuccess()
                    && response.getBody().getData() != null) {
                return response.getBody().getData();
            }

            throw new InventorySystemException(
                    "Failed to get inventory snapshot: " + response.getStatusCode());

        } catch (HttpClientErrorException e) {
            throw new InventorySystemException(
                    "Error getting inventory snapshot: " + e.getStatusCode(), e);
        } catch (HttpServerErrorException e) {
            throw new InventorySystemException(
                    "Error getting inventory snapshot: " + e.getStatusCode(), e);
        } catch (RestClientException e) {
            throw new InventorySystemException("Error communicating with inventory system", e);
        } catch (Exception e) {
            throw new InventorySystemException("Unexpected error getting inventory snapshot", e);
        }
    }
}
