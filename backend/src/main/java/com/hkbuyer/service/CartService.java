package com.hkbuyer.service;

import com.hkbuyer.domain.CartItem;
import com.hkbuyer.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CatalogService catalogService;

    public CartService(CartRepository cartRepository, CatalogService catalogService) {
        this.cartRepository = cartRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public Map<String, Object> upsertItem(Long userId, Long skuId, Integer qty) {
        catalogService.getSkuDetail(skuId);
        cartRepository.upsertItem(userId, skuId, qty);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", userId);
        payload.put("skuId", skuId);
        payload.put("qty", qty);
        payload.put("message", "cart item saved");
        return payload;
    }

    public List<Map<String, Object>> listItems(Long userId) {
        return cartRepository.listSelectedItems(userId).stream()
                .map(this::toPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> removeItem(Long userId, Long skuId) {
        int affected = cartRepository.removeItem(userId, skuId);
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", userId);
        payload.put("skuId", skuId);
        payload.put("removed", Boolean.valueOf(affected > 0));
        return payload;
    }

    private Map<String, Object> toPayload(CartItem item) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("cartItemId", item.getCartItemId());
        payload.put("userId", item.getUserId());
        payload.put("skuId", item.getSkuId());
        payload.put("qty", item.getQty());
        payload.put("selected", Boolean.valueOf(item.isSelected()));
        payload.put("updatedAt", item.getUpdatedAt());
        payload.put("sku", catalogService.getSkuDetail(item.getSkuId()));
        return payload;
    }
}
