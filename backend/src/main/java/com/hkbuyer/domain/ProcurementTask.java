package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProcurementTask {
    private Long taskId;
    private Long orderId;
    private Long buyerId;
    private TaskStatus taskStatus;
    private LocalDateTime publishAt;
    private LocalDateTime acceptDeadline;
    private BigDecimal suggestedMarkup;
    private TaskTier taskTier;
    private BuyerLevel requiredBuyerLevel;
    private String targetRegion;
    private String targetCategory;
    private Integer slaHours;
    private LocalDateTime acceptedAt;
    private LocalDateTime updatedAt;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public LocalDateTime getPublishAt() {
        return publishAt;
    }

    public void setPublishAt(LocalDateTime publishAt) {
        this.publishAt = publishAt;
    }

    public LocalDateTime getAcceptDeadline() {
        return acceptDeadline;
    }

    public void setAcceptDeadline(LocalDateTime acceptDeadline) {
        this.acceptDeadline = acceptDeadline;
    }

    public BigDecimal getSuggestedMarkup() {
        return suggestedMarkup;
    }

    public void setSuggestedMarkup(BigDecimal suggestedMarkup) {
        this.suggestedMarkup = suggestedMarkup;
    }

    public TaskTier getTaskTier() {
        return taskTier;
    }

    public void setTaskTier(TaskTier taskTier) {
        this.taskTier = taskTier;
    }

    public BuyerLevel getRequiredBuyerLevel() {
        return requiredBuyerLevel;
    }

    public void setRequiredBuyerLevel(BuyerLevel requiredBuyerLevel) {
        this.requiredBuyerLevel = requiredBuyerLevel;
    }

    public String getTargetRegion() {
        return targetRegion;
    }

    public void setTargetRegion(String targetRegion) {
        this.targetRegion = targetRegion;
    }

    public String getTargetCategory() {
        return targetCategory;
    }

    public void setTargetCategory(String targetCategory) {
        this.targetCategory = targetCategory;
    }

    public Integer getSlaHours() {
        return slaHours;
    }

    public void setSlaHours(Integer slaHours) {
        this.slaHours = slaHours;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
