package com.hkbuyer.api.dto;

import javax.validation.constraints.NotBlank;

public class CreateSpuRequest {

    @NotBlank
    private String spuName;

    @NotBlank
    private String brandName;

    @NotBlank
    private String categoryName;

    public String getSpuName() {
        return spuName;
    }

    public void setSpuName(String spuName) {
        this.spuName = spuName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
