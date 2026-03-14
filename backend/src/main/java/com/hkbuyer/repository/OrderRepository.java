package com.hkbuyer.repository;

import com.hkbuyer.domain.OrderItem;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createOrder(Long userId, BigDecimal totalAmount) {
        return createOrder(userId, totalAmount, null, null);
    }

    public long createOrder(Long userId, BigDecimal totalAmount, String appliedCouponCode, Long appliedCampaignId) {
        String sql = "INSERT INTO order_main(user_id, order_status, pay_status, total_amount, applied_coupon_code, applied_campaign_id, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId.longValue());
            ps.setString(2, OrderStatus.PENDING_PAYMENT.name());
            ps.setString(3, "UNPAID");
            ps.setBigDecimal(4, totalAmount);
            ps.setString(5, appliedCouponCode);
            if (appliedCampaignId == null) {
                ps.setObject(6, null);
            } else {
                ps.setLong(6, appliedCampaignId.longValue());
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void addItem(Long orderId, Long skuId, Integer qty, BigDecimal unitPrice, BigDecimal lineAmount) {
        String sql = "INSERT INTO order_item(order_id, sku_id, qty, unit_price, line_amount, created_at) VALUES(?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, orderId, skuId, qty, unitPrice, lineAmount);
    }

    public Optional<OrderMain> findById(Long orderId) {
        String sql = "SELECT order_id, user_id, order_status, pay_status, total_amount, applied_coupon_code, applied_campaign_id, created_at, updated_at " +
                "FROM order_main WHERE order_id = ?";
        List<OrderMain> result = jdbcTemplate.query(sql, orderMainRowMapper(), orderId);
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(result.get(0));
    }

    public List<OrderItem> findItems(Long orderId) {
        String sql = "SELECT order_item_id, order_id, sku_id, qty, unit_price, line_amount FROM order_item WHERE order_id = ?";
        return jdbcTemplate.query(sql, orderItemRowMapper(), orderId);
    }

    public void markPaid(Long orderId) {
        String sql = "UPDATE order_main SET pay_status = 'PAID', order_status = ?, updated_at = NOW() WHERE order_id = ?";
        jdbcTemplate.update(sql, OrderStatus.PAID_WAIT_ACCEPT.name(), orderId);
    }

    public void markPayFailed(Long orderId) {
        String sql = "UPDATE order_main SET pay_status = 'FAILED', order_status = ?, updated_at = NOW() WHERE order_id = ?";
        jdbcTemplate.update(sql, OrderStatus.PENDING_PAYMENT.name(), orderId);
    }

    public void updateStatus(Long orderId, OrderStatus status) {
        String sql = "UPDATE order_main SET order_status = ?, updated_at = NOW() WHERE order_id = ?";
        jdbcTemplate.update(sql, status.name(), orderId);
    }

    public long countPaidOrders() {
        String sql = "SELECT COUNT(1) FROM order_main WHERE pay_status = 'PAID'";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long countCancelledOrders() {
        String sql = "SELECT COUNT(1) FROM order_main WHERE order_status = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, OrderStatus.CANCELLED.name());
        return result == null ? 0L : result.longValue();
    }

    public long countTotalOrders() {
        String sql = "SELECT COUNT(1) FROM order_main";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long countPaidOrdersByUser(Long userId) {
        String sql = "SELECT COUNT(1) FROM order_main WHERE user_id = ? AND pay_status = 'PAID'";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, userId);
        return result == null ? 0L : result.longValue();
    }

    public BigDecimal sumPaidAmountByUser(Long userId) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM order_main WHERE user_id = ? AND pay_status = 'PAID'";
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, userId);
        return result == null ? BigDecimal.ZERO : result;
    }

    public long countPaidUsersWithinDays(int days) {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM order_main WHERE pay_status = 'PAID' " +
                "AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, Integer.valueOf(days));
        return result == null ? 0L : result.longValue();
    }

    public long countRepurchaseUsersWithinDays(int days) {
        String sql = "SELECT COUNT(1) FROM (" +
                "SELECT user_id FROM order_main WHERE pay_status = 'PAID' AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "GROUP BY user_id HAVING COUNT(1) >= 2" +
                ") t";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, Integer.valueOf(days));
        return result == null ? 0L : result.longValue();
    }

    public List<Long> listTopPurchasedSkuIdsByUser(Long userId, int limit) {
        String sql = "SELECT oi.sku_id FROM order_item oi " +
                "JOIN order_main om ON om.order_id = oi.order_id " +
                "WHERE om.user_id = ? AND om.pay_status = 'PAID' " +
                "GROUP BY oi.sku_id ORDER BY SUM(oi.qty) DESC, MAX(om.updated_at) DESC LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> Long.valueOf(rs.getLong("sku_id")), userId, Integer.valueOf(limit));
    }

    private RowMapper<OrderMain> orderMainRowMapper() {
        return (rs, rowNum) -> {
            OrderMain item = new OrderMain();
            item.setOrderId(rs.getLong("order_id"));
            item.setUserId(rs.getLong("user_id"));
            item.setOrderStatus(OrderStatus.valueOf(rs.getString("order_status")));
            item.setPayStatus(rs.getString("pay_status"));
            item.setTotalAmount(rs.getBigDecimal("total_amount"));
            item.setAppliedCouponCode(rs.getString("applied_coupon_code"));
            long appliedCampaignId = rs.getLong("applied_campaign_id");
            if (!rs.wasNull()) {
                item.setAppliedCampaignId(Long.valueOf(appliedCampaignId));
            }
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return item;
        };
    }

    private RowMapper<OrderItem> orderItemRowMapper() {
        return (rs, rowNum) -> {
            OrderItem item = new OrderItem();
            item.setOrderItemId(rs.getLong("order_item_id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setSkuId(rs.getLong("sku_id"));
            item.setQty(rs.getInt("qty"));
            item.setUnitPrice(rs.getBigDecimal("unit_price"));
            item.setLineAmount(rs.getBigDecimal("line_amount"));
            return item;
        };
    }
}
