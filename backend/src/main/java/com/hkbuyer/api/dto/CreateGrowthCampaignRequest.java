package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class CreateGrowthCampaignRequest {

    @NotNull
    private Long adminId;

    @NotBlank
    @Size(max = 120)
    private String campaignName;

    @NotBlank
    @Size(max = 20)
    private String targetMemberLevel;

    @NotBlank
    @Size(max = 40)
    private String couponCode;

    @NotBlank
    private String startAt;

    @NotBlank
    private String endAt;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getCampaignName() {
        return campaignName;
    }

    public void setCampaignName(String campaignName) {
        this.campaignName = campaignName;
    }

    public String getTargetMemberLevel() {
        return targetMemberLevel;
    }

    public void setTargetMemberLevel(String targetMemberLevel) {
        this.targetMemberLevel = targetMemberLevel;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }
}
