package com.hkbuyer.api.dto;

import javax.validation.constraints.NotNull;

public class ActivateOpsExperimentRequest {

    @NotNull
    private Long adminId;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }
}
