package com.hkbuyer.domain;

import java.time.LocalDateTime;

public class ShipmentTrackingRecord {
    private Long shipmentId;
    private Long orderId;
    private String carrier;
    private String trackingNo;
    private String shipmentStatus;
    private String latestNode;
    private LocalDateTime latestNodeAt;
    private LocalDateTime signedAt;
    private LocalDateTime updatedAt;

    public Long getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Long shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }

    public String getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getLatestNode() {
        return latestNode;
    }

    public void setLatestNode(String latestNode) {
        this.latestNode = latestNode;
    }

    public LocalDateTime getLatestNodeAt() {
        return latestNodeAt;
    }

    public void setLatestNodeAt(LocalDateTime latestNodeAt) {
        this.latestNodeAt = latestNodeAt;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
