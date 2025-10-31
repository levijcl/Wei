package com.wei.orchestrator.order.api.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrderResponse {
    private String orderId;
    private String status;
    private List<OrderLineItemDto> items;
    private ReservationInfoDto reservationInfo;
    private ShipmentInfoDto shipmentInfo;

    public OrderResponse() {}

    public OrderResponse(
            String orderId,
            String status,
            List<OrderLineItemDto> items,
            ReservationInfoDto reservationInfo,
            ShipmentInfoDto shipmentInfo) {
        this.orderId = orderId;
        this.status = status;
        this.items = items;
        this.reservationInfo = reservationInfo;
        this.shipmentInfo = shipmentInfo;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderLineItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderLineItemDto> items) {
        this.items = items;
    }

    public ReservationInfoDto getReservationInfo() {
        return reservationInfo;
    }

    public void setReservationInfo(ReservationInfoDto reservationInfo) {
        this.reservationInfo = reservationInfo;
    }

    public ShipmentInfoDto getShipmentInfo() {
        return shipmentInfo;
    }

    public void setShipmentInfo(ShipmentInfoDto shipmentInfo) {
        this.shipmentInfo = shipmentInfo;
    }

    public static class OrderLineItemDto {
        private String sku;
        private Integer quantity;
        private BigDecimal price;

        public OrderLineItemDto() {}

        public OrderLineItemDto(String sku, Integer quantity, BigDecimal price) {
            this.sku = sku;
            this.quantity = quantity;
            this.price = price;
        }

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

    public static class ReservationInfoDto {
        private String warehouseId;
        private Integer reservedQty;
        private String status;

        public ReservationInfoDto() {}

        public ReservationInfoDto(String warehouseId, Integer reservedQty, String status) {
            this.warehouseId = warehouseId;
            this.reservedQty = reservedQty;
            this.status = status;
        }

        public String getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(String warehouseId) {
            this.warehouseId = warehouseId;
        }

        public Integer getReservedQty() {
            return reservedQty;
        }

        public void setReservedQty(Integer reservedQty) {
            this.reservedQty = reservedQty;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ShipmentInfoDto {
        private String carrier;
        private String trackingNumber;

        public ShipmentInfoDto() {}

        public ShipmentInfoDto(String carrier, String trackingNumber) {
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
}
