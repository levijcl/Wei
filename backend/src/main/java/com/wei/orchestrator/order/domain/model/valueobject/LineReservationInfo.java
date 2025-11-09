package com.wei.orchestrator.order.domain.model.valueobject;

import java.time.LocalDateTime;
import java.util.Objects;

public final class LineReservationInfo {
    private final ReservationStatus status;
    private final String transactionId;
    private final String externalReservationId;
    private final String warehouseId;
    private final String failureReason;
    private final LocalDateTime reservedAt;

    private LineReservationInfo(
            ReservationStatus status,
            String transactionId,
            String externalReservationId,
            String warehouseId,
            String failureReason,
            LocalDateTime reservedAt) {
        this.status = status;
        this.transactionId = transactionId;
        this.externalReservationId = externalReservationId;
        this.warehouseId = warehouseId;
        this.failureReason = failureReason;
        this.reservedAt = reservedAt;
    }

    public static LineReservationInfo pending() {
        return new LineReservationInfo(ReservationStatus.PENDING, null, null, null, null, null);
    }

    public static LineReservationInfo reserved(
            String transactionId, String externalReservationId, String warehouseId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or blank");
        }
        if (externalReservationId == null || externalReservationId.isBlank()) {
            throw new IllegalArgumentException("External reservation ID cannot be null or blank");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new IllegalArgumentException("Warehouse ID cannot be null or blank");
        }
        return new LineReservationInfo(
                ReservationStatus.RESERVED,
                transactionId,
                externalReservationId,
                warehouseId,
                null,
                LocalDateTime.now());
    }

    public static LineReservationInfo failed(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Failure reason cannot be null or blank");
        }
        return new LineReservationInfo(
                ReservationStatus.FAILED, null, null, null, reason, LocalDateTime.now());
    }

    public boolean isReserved() {
        return status == ReservationStatus.RESERVED;
    }

    public boolean isFailed() {
        return status == ReservationStatus.FAILED;
    }

    public boolean isPending() {
        return status == ReservationStatus.PENDING;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getExternalReservationId() {
        return externalReservationId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineReservationInfo that = (LineReservationInfo) o;
        return status == that.status
                && Objects.equals(transactionId, that.transactionId)
                && Objects.equals(externalReservationId, that.externalReservationId)
                && Objects.equals(warehouseId, that.warehouseId)
                && Objects.equals(failureReason, that.failureReason)
                && Objects.equals(reservedAt, that.reservedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                status,
                transactionId,
                externalReservationId,
                warehouseId,
                failureReason,
                reservedAt);
    }

    @Override
    public String toString() {
        return String.format(
                "LineReservationInfo{status=%s, transactionId='%s', externalReservationId='%s',"
                        + " warehouseId='%s', failureReason='%s', reservedAt=%s}",
                status,
                transactionId,
                externalReservationId,
                warehouseId,
                failureReason,
                reservedAt);
    }
}
