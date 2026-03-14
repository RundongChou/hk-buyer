package com.hkbuyer.api;

import com.hkbuyer.api.dto.RemoveCartItemRequest;
import com.hkbuyer.api.dto.UpsertCartItemRequest;
import com.hkbuyer.service.CartService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/items")
    public List<Map<String, Object>> listItems(@RequestParam("userId") Long userId) {
        return cartService.listItems(userId);
    }

    @PostMapping("/items/upsert")
    public Map<String, Object> upsertItem(@Valid @RequestBody UpsertCartItemRequest request) {
        return cartService.upsertItem(request.getUserId(), request.getSkuId(), request.getQty());
    }

    @PostMapping("/items/remove")
    public Map<String, Object> removeItem(@Valid @RequestBody RemoveCartItemRequest request) {
        return cartService.removeItem(request.getUserId(), request.getSkuId());
    }
}
