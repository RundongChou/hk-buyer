package com.hkbuyer.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SettlementLedger {
    private Long ledgerId;
    private Long orderId;
    private Long taskId;
    private Long buyerId;
    private String buyerSettlementAccount;
    private BigDecimal orderAmount;
    private BigDecimal goodsCostAmount;
    private BigDecimal buyerIncomeAmount;
    private BigDecimal logisticsCostAmount;
    private BigDecimal platformServiceAmount;
    private String settlementStatus;
    private String reconciliationStatus;
    private String exceptionReason;
    private LocalDateTime signedAt;
    private LocalDateTime payoutRequestedAt;
    private LocalDateTime settledAt;
    private LocalDateTime reconciledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getLedgerId() {
        return ledgerId;
    }

    public void setLedgerId(Long ledgerId) {
        this.ledgerId = ledgerId;
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

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerSettlementAccount() {
        return buyerSettlementAccount;
    }

    public void setBuyerSettlementAccount(String buyerSettlementAccount) {
        this.buyerSettlementAccount = buyerSettlementAccount;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public BigDecimal getGoodsCostAmount() {
        return goodsCostAmount;
    }

    public void setGoodsCostAmount(BigDecimal goodsCostAmount) {
        this.goodsCostAmount = goodsCostAmount;
    }

    public BigDecimal getBuyerIncomeAmount() {
        return buyerIncomeAmount;
    }

    public void setBuyerIncomeAmount(BigDecimal buyerIncomeAmount) {
        this.buyerIncomeAmount = buyerIncomeAmount;
    }

    public BigDecimal getLogisticsCostAmount() {
        return logisticsCostAmount;
    }

    public void setLogisticsCostAmount(BigDecimal logisticsCostAmount) {
        this.logisticsCostAmount = logisticsCostAmount;
    }

    public BigDecimal getPlatformServiceAmount() {
        return platformServiceAmount;
    }

    public void setPlatformServiceAmount(BigDecimal platformServiceAmount) {
        this.platformServiceAmount = platformServiceAmount;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public String getExceptionReason() {
        return exceptionReason;
    }

    public void setExceptionReason(String exceptionReason) {
        this.exceptionReason = exceptionReason;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public LocalDateTime getPayoutRequestedAt() {
        return payoutRequestedAt;
    }

    public void setPayoutRequestedAt(LocalDateTime payoutRequestedAt) {
        this.payoutRequestedAt = payoutRequestedAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public LocalDateTime getReconciledAt() {
        return reconciledAt;
    }

    public void setReconciledAt(LocalDateTime reconciledAt) {
        this.reconciledAt = reconciledAt;
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
