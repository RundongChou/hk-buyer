package com.hkbuyer.api.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class PublishGrowthCampaignRequest {

    @NotNull
    private Long adminId;

    @NotEmpty
    private List<Long> userIds;

    @Size(max = 30)
    private String touchChannel;

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }

    public String getTouchChannel() {
        return touchChannel;
    }

    public void setTouchChannel(String touchChannel) {
        this.touchChannel = touchChannel;
    }
}
