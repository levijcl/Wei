package com.wei.orchestrator.inventory.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class InventorySnapshotResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("data")
    private List<InventorySnapshotDto> data;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<InventorySnapshotDto> getData() {
        return data;
    }

    public void setData(List<InventorySnapshotDto> data) {
        this.data = data;
    }
}
