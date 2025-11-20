package com.wei.orchestrator.order.query.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ProcessStepDetailDto {

    private Integer stepNumber;
    private String stepName;
    private String overallStatus;
    private List<EventDetailDto> events;

    public ProcessStepDetailDto() {}

    public ProcessStepDetailDto(
            Integer stepNumber,
            String stepName,
            String overallStatus,
            List<EventDetailDto> events) {
        this.stepNumber = stepNumber;
        this.stepName = stepName;
        this.overallStatus = overallStatus;
        this.events = events;
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

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public List<EventDetailDto> getEvents() {
        return events;
    }

    public void setEvents(List<EventDetailDto> events) {
        this.events = events;
    }

    public static class EventDetailDto {
        private String recordId;
        private String eventName;
        private LocalDateTime eventTimestamp;
        private EventMetadataDto metadata;
        private Object payload;

        public EventDetailDto() {}

        public EventDetailDto(
                String recordId,
                String eventName,
                LocalDateTime eventTimestamp,
                EventMetadataDto metadata,
                Object payload) {
            this.recordId = recordId;
            this.eventName = eventName;
            this.eventTimestamp = eventTimestamp;
            this.metadata = metadata;
            this.payload = payload;
        }

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public LocalDateTime getEventTimestamp() {
            return eventTimestamp;
        }

        public void setEventTimestamp(LocalDateTime eventTimestamp) {
            this.eventTimestamp = eventTimestamp;
        }

        public EventMetadataDto getMetadata() {
            return metadata;
        }

        public void setMetadata(EventMetadataDto metadata) {
            this.metadata = metadata;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }
    }

    public static class EventMetadataDto {
        private String context;
        private String correlationId;
        private String triggerSource;
        private String triggerBy;

        public EventMetadataDto() {}

        public EventMetadataDto(
                String context, String correlationId, String triggerSource, String triggerBy) {
            this.context = context;
            this.correlationId = correlationId;
            this.triggerSource = triggerSource;
            this.triggerBy = triggerBy;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public void setCorrelationId(String correlationId) {
            this.correlationId = correlationId;
        }

        public String getTriggerSource() {
            return triggerSource;
        }

        public void setTriggerSource(String triggerSource) {
            this.triggerSource = triggerSource;
        }

        public String getTriggerBy() {
            return triggerBy;
        }

        public void setTriggerBy(String triggerBy) {
            this.triggerBy = triggerBy;
        }
    }
}
