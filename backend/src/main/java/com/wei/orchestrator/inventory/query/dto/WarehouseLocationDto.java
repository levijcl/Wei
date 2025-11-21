package com.wei.orchestrator.inventory.query.dto;

public class WarehouseLocationDto {

    private String warehouseId;
    private String zone;

    public WarehouseLocationDto() {}

    public WarehouseLocationDto(String warehouseId, String zone) {
        this.warehouseId = warehouseId;
        this.zone = zone;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
