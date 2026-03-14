package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AfterSaleCase {
    private Long caseId;
    private Long orderId;
    private Long taskId;
    private Long userId;
    private Long buyerId;
    private String caseType;
    private String caseStatus;
    private String issueReason;
    private String evidenceUrl;
    private String replacementSkuName;
    private BigDecimal suggestedRefundAmount;
    private BigDecimal negotiatedRefundAmount;
    private String userDecision;
    private String userComment;
    private String arbitrationResult;
    private String riskLevel;
    private String originOrderStatus;
    private Long adminId;
    private String arbitrationComment;
    private LocalDateTime userDecisionAt;
    private LocalDateTime arbitratedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(String caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getIssueReason() {
        return issueReason;
    }

    public void setIssueReason(String issueReason) {
        this.issueReason = issueReason;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
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

    public BigDecimal getNegotiatedRefundAmount() {
        return negotiatedRefundAmount;
    }

    public void setNegotiatedRefundAmount(BigDecimal negotiatedRefundAmount) {
        this.negotiatedRefundAmount = negotiatedRefundAmount;
    }

    public String getUserDecision() {
        return userDecision;
    }

    public void setUserDecision(String userDecision) {
        this.userDecision = userDecision;
    }

    public String getUserComment() {
        return userComment;
    }

    public void setUserComment(String userComment) {
        this.userComment = userComment;
    }

    public String getArbitrationResult() {
        return arbitrationResult;
    }

    public void setArbitrationResult(String arbitrationResult) {
        this.arbitrationResult = arbitrationResult;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getOriginOrderStatus() {
        return originOrderStatus;
    }

    public void setOriginOrderStatus(String originOrderStatus) {
        this.originOrderStatus = originOrderStatus;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getArbitrationComment() {
        return arbitrationComment;
    }

    public void setArbitrationComment(String arbitrationComment) {
        this.arbitrationComment = arbitrationComment;
    }

    public LocalDateTime getUserDecisionAt() {
        return userDecisionAt;
    }

    public void setUserDecisionAt(LocalDateTime userDecisionAt) {
        this.userDecisionAt = userDecisionAt;
    }

    public LocalDateTime getArbitratedAt() {
        return arbitratedAt;
    }

    public void setArbitratedAt(LocalDateTime arbitratedAt) {
        this.arbitratedAt = arbitratedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
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
