package com.wei.orchestrator.inventory.domain.port;

import com.wei.orchestrator.inventory.domain.exception.InsufficientInventoryException;
import com.wei.orchestrator.inventory.domain.exception.InventorySystemException;
import com.wei.orchestrator.inventory.domain.exception.ReservationNotFoundException;
import com.wei.orchestrator.inventory.domain.model.valueobject.ExternalReservationId;

public interface InventoryPort {

    ExternalReservationId createReservation(
            String sku, String warehouseId, String orderId, int quantity)
            throws InsufficientInventoryException, InventorySystemException;

    void consumeReservation(ExternalReservationId reservationId)
            throws ReservationNotFoundException, InventorySystemException;

    void releaseReservation(ExternalReservationId reservationId)
            throws ReservationNotFoundException, InventorySystemException;

    void increaseInventory(String sku, String warehouseId, int quantity, String reason)
            throws InventorySystemException;

    void adjustInventory(String sku, String warehouseId, int quantityChange, String reason)
            throws InventorySystemException;
}
