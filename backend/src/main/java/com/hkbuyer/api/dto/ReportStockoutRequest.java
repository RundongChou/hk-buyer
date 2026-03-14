package com.hkbuyer.api.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class ReportStockoutRequest {

    @NotNull
    private Long buyerId;

    @NotBlank
    @Size(max = 255)
    private String issueReason;

    @Size(max = 120)
    private String replacementSkuName;

    @DecimalMin("0.00")
    private BigDecimal suggestedRefundAmount;

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public void setIssueReason(String issueReason) {
        this.issueReason = issueReason;
    }

    public String getReplacementSkuName() {
        return replacementSkuName;
    }

    public void setReplacementSkuName(String replacementSkuName) {
        this.replacementSkuName = replacementSkuName;
    }

    public BigDecimal getSuggestedRefundAmount() {
        return suggestedRefundAmount;
    }

    public void setSuggestedRefundAmount(BigDecimal suggestedRefundAmount) {
        this.suggestedRefundAmount = suggestedRefundAmount;
    }
}
