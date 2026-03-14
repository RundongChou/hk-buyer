package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class SubmitBuyerOnboardingRequest {

    @NotNull
    private Long buyerId;

    @NotBlank
    @Size(max = 60)
    private String realName;

    @NotBlank
    @Size(min = 4, max = 8)
    private String idCardSuffix;

    @NotBlank
    @Size(max = 40)
    private String serviceRegion;

    @NotBlank
    @Size(max = 60)
    private String specialtyCategory;

    @NotBlank
    @Size(max = 120)
    private String settlementAccount;

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
}
