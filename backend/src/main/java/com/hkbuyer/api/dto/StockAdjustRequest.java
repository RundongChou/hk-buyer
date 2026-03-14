package com.hkbuyer.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class StockAdjustRequest {

    @NotNull
    @Min(0)
    private Integer availableQty;

    @Min(0)
    private Integer alertThreshold;

    private String reason;

    public Integer getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
