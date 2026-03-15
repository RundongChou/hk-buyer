package com.hkbuyer.api.dto;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;

public class CreateOpsExperimentRequest {

    @NotNull
    private Long adminId;

    @NotBlank
    @Size(max = 64)
    private String experimentKey;

    @NotBlank
    @Size(max = 120)
    private String experimentName;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal controlRatio;

    @NotNull
    @DecimalMin("0.0000")
    @DecimalMax("1.0000")
    private BigDecimal treatmentRatio;

    @NotNull
    private BigDecimal treatmentMarkupDelta;

    @NotNull
    @Min(24)
    @Max(96)
    private Integer treatmentSlaHours;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getExperimentKey() {
        return experimentKey;
    }

    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public BigDecimal getControlRatio() {
        return controlRatio;
    }

    public void setControlRatio(BigDecimal controlRatio) {
        this.controlRatio = controlRatio;
    }

    public BigDecimal getTreatmentRatio() {
        return treatmentRatio;
    }

    public void setTreatmentRatio(BigDecimal treatmentRatio) {
        this.treatmentRatio = treatmentRatio;
    }

    public BigDecimal getTreatmentMarkupDelta() {
        return treatmentMarkupDelta;
    }

    public void setTreatmentMarkupDelta(BigDecimal treatmentMarkupDelta) {
        this.treatmentMarkupDelta = treatmentMarkupDelta;
    }

    public Integer getTreatmentSlaHours() {
        return treatmentSlaHours;
    }

    public void setTreatmentSlaHours(Integer treatmentSlaHours) {
        this.treatmentSlaHours = treatmentSlaHours;
    }
}
