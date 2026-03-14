package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class SubmitCustomsRequest {

    @NotNull
    private Long orderId;

    @NotBlank
    @Size(max = 64)
    private String declarationNo;

    @Size(max = 40)
    private String complianceChannel;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getDeclarationNo() {
        return declarationNo;
    }

    public void setDeclarationNo(String declarationNo) {
        this.declarationNo = declarationNo;
    }

    public String getComplianceChannel() {
        return complianceChannel;
    }

    public void setComplianceChannel(String complianceChannel) {
        this.complianceChannel = complianceChannel;
    }
}
