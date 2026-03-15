package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OpsExperimentAssignment {

    private Long assignmentId;
    private Long experimentId;
    private Long orderId;
    private Long userId;
    private OpsVariant variant;
    private Integer baseSlaHours;
    private Integer finalSlaHours;
    private BigDecimal baseMarkup;
    private BigDecimal finalMarkup;
    private LocalDateTime assignedAt;

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OpsVariant getVariant() {
        return variant;
    }

    public void setVariant(OpsVariant variant) {
        this.variant = variant;
    }

    public Integer getBaseSlaHours() {
        return baseSlaHours;
    }

    public void setBaseSlaHours(Integer baseSlaHours) {
        this.baseSlaHours = baseSlaHours;
    }

    public Integer getFinalSlaHours() {
        return finalSlaHours;
    }

    public void setFinalSlaHours(Integer finalSlaHours) {
        this.finalSlaHours = finalSlaHours;
    }

    public BigDecimal getBaseMarkup() {
        return baseMarkup;
    }

    public void setBaseMarkup(BigDecimal baseMarkup) {
        this.baseMarkup = baseMarkup;
    }

    public BigDecimal getFinalMarkup() {
        return finalMarkup;
    }

    public void setFinalMarkup(BigDecimal finalMarkup) {
        this.finalMarkup = finalMarkup;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}
