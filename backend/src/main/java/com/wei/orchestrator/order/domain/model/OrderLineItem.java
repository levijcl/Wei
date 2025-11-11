package com.wei.orchestrator.order.domain.model;

import com.wei.orchestrator.order.domain.model.valueobject.LineCommitmentInfo;
import com.wei.orchestrator.order.domain.model.valueobject.LineReservationInfo;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class OrderLineItem {
    private String lineItemId;
    private String sku;
    private Integer quantity;
    private BigDecimal price;
    private LineReservationInfo reservationInfo;
    private LineCommitmentInfo commitmentInfo;

    public OrderLineItem() {
        this.lineItemId = UUID.randomUUID().toString();
    }

    public OrderLineItem(String sku, Integer quantity, BigDecimal price) {
        this.lineItemId = UUID.randomUUID().toString();
        this.sku = sku;
        this.quantity = quantity;
        this.price = price;
    }

    public void reserveItem(
            String transactionId, String externalReservationId, String warehouseId) {
        if (this.reservationInfo != null && this.reservationInfo.isReserved()) {
            throw new IllegalStateException("Line item " + lineItemId + " is already reserved");
        }
        this.reservationInfo =
                LineReservationInfo.reserved(transactionId, externalReservationId, warehouseId);
    }

    public void markReservationFailed(String reason) {
        if (this.reservationInfo != null && this.reservationInfo.isReserved()) {
            throw new IllegalStateException(
                    "Cannot mark already reserved line item " + lineItemId + " as failed");
        }
        this.reservationInfo = LineReservationInfo.failed(reason);
    }

    public void markPickingInProgress(String pickingTaskId) {
        this.commitmentInfo = LineCommitmentInfo.inProgress(pickingTaskId);
    }

    public void commitItem(String wesTransactionId) {
        if (!isReserved()) {
            throw new IllegalStateException("Cannot commit unreserved line item " + lineItemId);
        }
        if (this.commitmentInfo != null && this.commitmentInfo.isCommitted()) {
            throw new IllegalStateException("Line item " + lineItemId + " is already committed");
        }
        this.commitmentInfo = LineCommitmentInfo.committed(wesTransactionId);
    }

    public void markCommitmentFailed(String reason) {
        if (!isReserved()) {
            throw new IllegalStateException(
                    "Cannot mark commitment failed for unreserved line item " + lineItemId);
        }
        if (this.commitmentInfo != null && this.commitmentInfo.isCommitted()) {
            throw new IllegalStateException(
                    "Cannot mark already committed line item " + lineItemId + " as failed");
        }
        this.commitmentInfo = LineCommitmentInfo.failed(reason);
    }

    public boolean isReserved() {
        return reservationInfo != null && reservationInfo.isReserved();
    }

    public boolean isCommitted() {
        return commitmentInfo != null && commitmentInfo.isCommitted();
    }

    public boolean hasReservationFailed() {
        return reservationInfo != null && reservationInfo.isFailed();
    }

    public boolean hasCommitmentFailed() {
        return commitmentInfo != null && commitmentInfo.isFailed();
    }

    public String getLineItemId() {
        return lineItemId;
    }

    public void setLineItemId(String lineItemId) {
        this.lineItemId = lineItemId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LineReservationInfo getReservationInfo() {
        return reservationInfo;
    }

    public void setReservationInfo(LineReservationInfo reservationInfo) {
        this.reservationInfo = reservationInfo;
    }

    public LineCommitmentInfo getCommitmentInfo() {
        return commitmentInfo;
    }

    public void setCommitmentInfo(LineCommitmentInfo commitmentInfo) {
        this.commitmentInfo = commitmentInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderLineItem that = (OrderLineItem) o;
        return Objects.equals(lineItemId, that.lineItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineItemId);
    }

    @Override
    public String toString() {
        return String.format(
                "OrderLineItem{lineItemId='%s', sku='%s', quantity=%d, price=%s,"
                        + " reservationInfo=%s, commitmentInfo=%s}",
                lineItemId, sku, quantity, price, reservationInfo, commitmentInfo);
    }
}
