package com.wei.orchestrator.order.query.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderProcessStatusDto {

    private String orderId;
    private List<ProcessStepDto> steps;

    public OrderProcessStatusDto() {}

    public OrderProcessStatusDto(String orderId, List<ProcessStepDto> steps) {
        this.orderId = orderId;
        this.steps = steps;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<ProcessStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<ProcessStepDto> steps) {
        this.steps = steps;
    }

    public static class ProcessStepDto {
        private Integer stepNumber;
        private String stepName;
        private String status;
        private LocalDateTime timestamp;

        public ProcessStepDto() {}

        public ProcessStepDto(
                Integer stepNumber, String stepName, String status, LocalDateTime timestamp) {
            this.stepNumber = stepNumber;
            this.stepName = stepName;
            this.status = status;
            this.timestamp = timestamp;
        }

        public Integer getStepNumber() {
            return stepNumber;
        }

        public void setStepNumber(Integer stepNumber) {
            this.stepNumber = stepNumber;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }
}
