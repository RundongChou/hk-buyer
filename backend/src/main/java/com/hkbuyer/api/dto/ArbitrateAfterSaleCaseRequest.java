package com.hkbuyer.api.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class ArbitrateAfterSaleCaseRequest {

    @NotNull
    private Long adminId;

    @NotBlank
    @Size(max = 60)
    private String decision;

    @Size(max = 255)
    private String comment;

    @DecimalMin("0.00")
    private BigDecimal finalRefundAmount;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BigDecimal getFinalRefundAmount() {
        return finalRefundAmount;
    }

    public void setFinalRefundAmount(BigDecimal finalRefundAmount) {
        this.finalRefundAmount = finalRefundAmount;
    }
}
