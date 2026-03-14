package com.hkbuyer.repository;

import com.hkbuyer.domain.CartItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@Repository
public class CartRepository {

    private final JdbcTemplate jdbcTemplate;

    public CartRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertItem(Long userId, Long skuId, Integer qty) {
        String sql = "INSERT INTO user_cart_item(user_id, sku_id, qty, selected, created_at, updated_at) " +
                "VALUES(?, ?, ?, 1, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE qty = VALUES(qty), selected = 1, updated_at = NOW()";
        jdbcTemplate.update(sql, userId, skuId, qty);
    }

    public List<CartItem> listSelectedItems(Long userId) {
        String sql = "SELECT cart_item_id, user_id, sku_id, qty, selected, updated_at " +
                "FROM user_cart_item WHERE user_id = ? AND selected = 1 ORDER BY updated_at DESC, cart_item_id DESC";
        return jdbcTemplate.query(sql, cartItemRowMapper(), userId);
    }

    public int removeItem(Long userId, Long skuId) {
        String sql = "DELETE FROM user_cart_item WHERE user_id = ? AND sku_id = ?";
        return jdbcTemplate.update(sql, userId, skuId);
    }

    public int removeItems(Long userId, List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return 0;
        }

        String placeholders = String.join(",", Collections.nCopies(skuIds.size(), "?"));
        String sql = "DELETE FROM user_cart_item WHERE user_id = ? AND sku_id IN (" + placeholders + ")";

        Object[] args = new Object[skuIds.size() + 1];
        args[0] = userId;
        for (int i = 0; i < skuIds.size(); i++) {
            args[i + 1] = skuIds.get(i);
        }
        return jdbcTemplate.update(sql, args);
    }

    private RowMapper<CartItem> cartItemRowMapper() {
        return (rs, rowNum) -> {
            CartItem item = new CartItem();
            item.setCartItemId(rs.getLong("cart_item_id"));
            item.setUserId(rs.getLong("user_id"));
            item.setSkuId(rs.getLong("sku_id"));
            item.setQty(rs.getInt("qty"));
            item.setSelected(rs.getInt("selected") == 1);
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return item;
        };
    }
}
