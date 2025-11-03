package com.wei.orchestrator.wes.application.command;

import com.wei.orchestrator.wes.infrastructure.adapter.dto.WesTaskDto;

public class CreatePickingTaskFromWesCommand {

    private final WesTaskDto wesTask;

    public CreatePickingTaskFromWesCommand(WesTaskDto wesTask) {
        this.wesTask = wesTask;
    }

    public WesTaskDto getWesTask() {
        return wesTask;
    }
}
