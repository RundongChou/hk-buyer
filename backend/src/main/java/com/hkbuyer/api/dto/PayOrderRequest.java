package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;

public class PayOrderRequest {

    @NotBlank
    private String paymentChannel;

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }
}
