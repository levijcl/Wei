package com.wei.orchestrator.wes.application.command;

import java.util.List;

public class AdjustOrderPriorityCommand {

    private final String orderId;
    private final int newPriority;
    private final List<String> taskIds;

    public AdjustOrderPriorityCommand(String orderId, int newPriority, List<String> taskIds) {
        this.orderId = orderId;
        this.newPriority = newPriority;
        this.taskIds = taskIds;
    }

    public AdjustOrderPriorityCommand(String orderId, int newPriority) {
        this(orderId, newPriority, null);
    }

    public String getOrderId() {
        return orderId;
    }

    public int getNewPriority() {
        return newPriority;
    }

    public List<String> getTaskIds() {
        return taskIds;
    }

    public boolean isApplyToAll() {
        return taskIds == null || taskIds.isEmpty();
    }
}
