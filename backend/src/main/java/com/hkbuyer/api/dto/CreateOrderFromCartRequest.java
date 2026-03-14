package com.hkbuyer.api.dto;

import javax.validation.constraints.NotNull;

public class CreateOrderFromCartRequest {

    @NotNull
    private Long userId;

    private String couponCode;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
