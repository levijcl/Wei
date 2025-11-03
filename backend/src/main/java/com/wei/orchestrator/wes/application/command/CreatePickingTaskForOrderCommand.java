package com.wei.orchestrator.wes.application.command;

import com.wei.orchestrator.wes.application.command.dto.TaskItemDto;
import java.util.List;

public class CreatePickingTaskForOrderCommand {

    private final String orderId;
    private final List<TaskItemDto> items;
    private final int priority;

    public CreatePickingTaskForOrderCommand(String orderId, List<TaskItemDto> items, int priority) {
        this.orderId = orderId;
        this.items = items;
        this.priority = priority;
    }

    public String getOrderId() {
        return orderId;
    }

    public List<TaskItemDto> getItems() {
        return items;
    }

    public int getPriority() {
        return priority;
    }
}
