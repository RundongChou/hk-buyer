package com.hkbuyer.api.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CompleteSettlementPayoutRequest {

    @NotNull
    private Long adminId;

    @Size(max = 255)
    private String comment;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
