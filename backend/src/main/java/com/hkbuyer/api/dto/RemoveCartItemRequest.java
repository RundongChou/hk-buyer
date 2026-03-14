package com.hkbuyer.api.dto;

import javax.validation.constraints.NotNull;

public class RemoveCartItemRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long skuId;

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
}
