package com.hkbuyer.domain;

import java.time.LocalDateTime;

public class BuyerProfile {
    private Long buyerId;
    private String displayName;
    private BuyerAuditStatus auditStatus;
    private BuyerLevel buyerLevel;
    private Integer creditScore;
    private Integer rewardPoints;
    private Integer penaltyPoints;
    private Integer acceptedTaskCount;
    private Integer approvedProofCount;
    private Integer rejectedProofCount;
    private String serviceRegion;
    private String specialtyCategory;
    private String settlementAccount;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BuyerAuditStatus getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(BuyerAuditStatus auditStatus) {
        this.auditStatus = auditStatus;
    }

    public BuyerLevel getBuyerLevel() {
        return buyerLevel;
    }

    public void setBuyerLevel(BuyerLevel buyerLevel) {
        this.buyerLevel = buyerLevel;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public Integer getRewardPoints() {
        return rewardPoints;
    }

    public void setRewardPoints(Integer rewardPoints) {
        this.rewardPoints = rewardPoints;
    }

    public Integer getPenaltyPoints() {
        return penaltyPoints;
    }

    public void setPenaltyPoints(Integer penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }

    public Integer getAcceptedTaskCount() {
        return acceptedTaskCount;
    }

    public void setAcceptedTaskCount(Integer acceptedTaskCount) {
        this.acceptedTaskCount = acceptedTaskCount;
    }

    public Integer getApprovedProofCount() {
        return approvedProofCount;
    }

    public void setApprovedProofCount(Integer approvedProofCount) {
        this.approvedProofCount = approvedProofCount;
    }

    public Integer getRejectedProofCount() {
        return rejectedProofCount;
    }

    public void setRejectedProofCount(Integer rejectedProofCount) {
        this.rejectedProofCount = rejectedProofCount;
    }

    public String getServiceRegion() {
        return serviceRegion;
    }

    public void setServiceRegion(String serviceRegion) {
        this.serviceRegion = serviceRegion;
    }

    public String getSpecialtyCategory() {
        return specialtyCategory;
    }

    public void setSpecialtyCategory(String specialtyCategory) {
        this.specialtyCategory = specialtyCategory;
    }

    public String getSettlementAccount() {
        return settlementAccount;
    }

    public void setSettlementAccount(String settlementAccount) {
        this.settlementAccount = settlementAccount;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
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
