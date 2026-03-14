package com.hkbuyer.domain;

import java.time.LocalDateTime;

public class BuyerOnboardingApplication {
    private Long applicationId;
    private Long buyerId;
    private String realName;
    private String idCardSuffix;
    private String serviceRegion;
    private String specialtyCategory;
    private String settlementAccount;
    private BuyerAuditStatus applicationStatus;
    private Long reviewedBy;
    private String reviewComment;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime updatedAt;

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCardSuffix() {
        return idCardSuffix;
    }

    public void setIdCardSuffix(String idCardSuffix) {
        this.idCardSuffix = idCardSuffix;
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

    public BuyerAuditStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(BuyerAuditStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
