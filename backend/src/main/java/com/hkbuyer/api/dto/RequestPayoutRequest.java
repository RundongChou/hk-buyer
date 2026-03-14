package com.hkbuyer.api.dto;

import javax.validation.constraints.NotNull;

public class RequestPayoutRequest {

    @NotNull
    private Long buyerId;

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }
}
