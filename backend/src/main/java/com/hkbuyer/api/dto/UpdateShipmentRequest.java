package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UpdateShipmentRequest {

    @NotNull
    private Long orderId;

    @NotBlank
    @Size(max = 60)
    private String carrier;

    @NotBlank
    @Size(max = 80)
    private String trackingNo;

    @NotBlank
    @Size(max = 30)
    private String shipmentStatus;

    @NotBlank
    @Size(max = 120)
    private String latestNode;

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
}
