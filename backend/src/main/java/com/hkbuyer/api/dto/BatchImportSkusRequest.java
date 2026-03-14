package com.hkbuyer.api.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class BatchImportSkusRequest {

    @NotNull
    private Long spuId;

    @Valid
    @NotEmpty
    private List<BatchImportSkuItemRequest> items;

    public Long getSpuId() {
        return spuId;
    }

    public void setSpuId(Long spuId) {
        this.spuId = spuId;
    }

    public List<BatchImportSkuItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BatchImportSkuItemRequest> items) {
        this.items = items;
    }
}
