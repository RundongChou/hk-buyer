package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AuditProofRequest {

    @NotNull
    private Long adminId;

    @NotBlank
    private String decision;

    private String comment;

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
}
