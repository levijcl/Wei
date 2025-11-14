package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WesInventoryResponse {

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("warehouse_id")
    private String warehouseId;

    @JsonProperty("inventory")
    private List<WesInventoryDto> inventory;

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<WesInventoryDto> getInventory() {
        return inventory;
    }

    public void setInventory(List<WesInventoryDto> inventory) {
        this.inventory = inventory;
    }
}
