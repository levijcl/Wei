package com.wei.orchestrator.order.domain.model;

public class ShipmentInfo {
    private String carrier;
    private String trackingNumber;

    public ShipmentInfo() {}

    public ShipmentInfo(String carrier, String trackingNumber) {
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}
