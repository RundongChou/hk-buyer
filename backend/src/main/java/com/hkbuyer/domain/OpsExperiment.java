package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpsExperiment {

    private Long experimentId;
    private String experimentKey;
    private String experimentName;
    private String experimentStatus;
    private BigDecimal controlRatio;
    private BigDecimal treatmentRatio;
    private BigDecimal treatmentMarkupDelta;
    private Integer treatmentSlaHours;
    private Long createdBy;
    private Long activatedBy;
    private LocalDateTime activatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
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

    public String getExperimentStatus() {
        return experimentStatus;
    }

    public void setExperimentStatus(String experimentStatus) {
        this.experimentStatus = experimentStatus;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getActivatedBy() {
        return activatedBy;
    }

    public void setActivatedBy(Long activatedBy) {
        this.activatedBy = activatedBy;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
