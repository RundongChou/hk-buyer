package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CatalogSku {
    private Long skuId;
    private Long spuId;
    private String spuName;
    private String skuName;
    private String specValue;
    private String brandName;
    private CatalogPublishStatus publishStatus;
    private BigDecimal basePrice;
    private BigDecimal serviceFeeRate;
    private BigDecimal finalPrice;
    private Integer availableQty;
    private Integer lockedQty;
    private Integer alertThreshold;
    private LocalDateTime updatedAt;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Long getSpuId() {
        return spuId;
    }

    public void setSpuId(Long spuId) {
        this.spuId = spuId;
    }

    public String getSpuName() {
        return spuName;
    }

    public void setSpuName(String spuName) {
        this.spuName = spuName;
    }

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

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public CatalogPublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(CatalogPublishStatus publishStatus) {
        this.publishStatus = publishStatus;
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

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public Integer getAvailableQty() {
        return availableQty;
    }

    public void setAvailableQty(Integer availableQty) {
        this.availableQty = availableQty;
    }

    public Integer getLockedQty() {
        return lockedQty;
    }

    public void setLockedQty(Integer lockedQty) {
        this.lockedQty = lockedQty;
    }

    public Integer getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Integer alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
