package com.hkbuyer.api.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class CreateOrderRequest {

    @NotNull
    private Long userId;

    @Valid
    @NotEmpty
    private List<CreateOrderItemRequest> items;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CreateOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<CreateOrderItemRequest> items) {
        this.items = items;
    }
}
