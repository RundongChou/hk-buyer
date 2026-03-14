package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;

public class CompensatePayRequest {

    @NotBlank
    private String paymentChannel;

    @NotBlank
    private String compensationToken;

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(String paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getCompensationToken() {
        return compensationToken;
    }

    public void setCompensationToken(String compensationToken) {
        this.compensationToken = compensationToken;
    }
}
