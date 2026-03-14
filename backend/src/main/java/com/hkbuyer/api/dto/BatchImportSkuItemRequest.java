package com.hkbuyer.api.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BatchImportSkuItemRequest {

    @NotBlank
    private String skuName;

    @NotBlank
    private String specValue;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    @NotNull
    @DecimalMin("0.0000")
    private BigDecimal serviceFeeRate;

    @NotNull
    @Min(0)
    private Integer availableQty;

    @Min(0)
    private Integer alertThreshold;

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSpecValue() {
        return specValue;
    }

    public void setSpecValue(String specValue) {
        this.specValue = specValue;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getServiceFeeRate() {
        return serviceFeeRate;
    }

    public void setServiceFeeRate(BigDecimal serviceFeeRate) {
        this.serviceFeeRate = serviceFeeRate;
    }

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
}
