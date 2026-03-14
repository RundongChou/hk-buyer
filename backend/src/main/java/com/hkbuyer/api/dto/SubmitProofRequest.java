package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SubmitProofRequest {

    @NotNull
    private Long buyerId;

    @NotBlank
    private String storeName;

    @NotBlank
    private String receiptUrl;

    @NotBlank
    private String batchNo;

    @NotBlank
    private String expiryDate;

    @NotBlank
    private String productPhotoUrl;

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public void setReceiptUrl(String receiptUrl) {
        this.receiptUrl = receiptUrl;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getProductPhotoUrl() {
        return productPhotoUrl;
    }

    public void setProductPhotoUrl(String productPhotoUrl) {
        this.productPhotoUrl = productPhotoUrl;
    }
}
