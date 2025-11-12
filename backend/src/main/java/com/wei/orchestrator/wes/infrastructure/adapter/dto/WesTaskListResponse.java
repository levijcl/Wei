package com.wei.orchestrator.wes.infrastructure.adapter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WesTaskListResponse {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("tasks")
    private List<WesTaskDto> tasks;

    public WesTaskListResponse() {}

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<WesTaskDto> getTasks() {
        return tasks;
    }

    public void setTasks(List<WesTaskDto> tasks) {
        this.tasks = tasks;
    }
}
