package com.wei.orchestrator.order.domain.model;

public class ReservationInfo {
    private String warehouseId;
    private Integer reservedQty;
    private String status;

    public ReservationInfo() {}

    public ReservationInfo(String warehouseId, Integer reservedQty, String status) {
        this.warehouseId = warehouseId;
        this.reservedQty = reservedQty;
        this.status = status;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getReservedQty() {
        return reservedQty;
    }

    public void setReservedQty(Integer reservedQty) {
        this.reservedQty = reservedQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
