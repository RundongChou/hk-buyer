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
        String sql = "INSERT INTO order_main(user_id, order_status, pay_status, total_amount, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, userId.longValue());
            ps.setString(2, OrderStatus.PENDING_PAYMENT.name());
            ps.setString(3, "UNPAID");
            ps.setBigDecimal(4, totalAmount);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void addItem(Long orderId, Long skuId, Integer qty, BigDecimal unitPrice, BigDecimal lineAmount) {
        String sql = "INSERT INTO order_item(order_id, sku_id, qty, unit_price, line_amount, created_at) VALUES(?, ?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, orderId, skuId, qty, unitPrice, lineAmount);
    }

    public Optional<OrderMain> findById(Long orderId) {
        String sql = "SELECT order_id, user_id, order_status, pay_status, total_amount, created_at, updated_at FROM order_main WHERE order_id = ?";
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

    private RowMapper<OrderMain> orderMainRowMapper() {
        return (rs, rowNum) -> {
            OrderMain item = new OrderMain();
            item.setOrderId(rs.getLong("order_id"));
            item.setUserId(rs.getLong("user_id"));
            item.setOrderStatus(OrderStatus.valueOf(rs.getString("order_status")));
            item.setPayStatus(rs.getString("pay_status"));
            item.setTotalAmount(rs.getBigDecimal("total_amount"));
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
