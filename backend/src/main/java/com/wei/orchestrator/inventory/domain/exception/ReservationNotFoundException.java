package com.wei.orchestrator.inventory.domain.exception;

public class ReservationNotFoundException extends RuntimeException {
    private final String reservationId;

    public ReservationNotFoundException(String reservationId) {
        super("Reservation not found: " + reservationId);
        this.reservationId = reservationId;
    }

    public String getReservationId() {
        return reservationId;
    }
}
