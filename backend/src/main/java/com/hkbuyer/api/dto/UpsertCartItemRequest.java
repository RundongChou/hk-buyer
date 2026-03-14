package com.hkbuyer.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class UpsertCartItemRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer qty;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
}
