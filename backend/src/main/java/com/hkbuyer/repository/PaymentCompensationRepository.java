package com.hkbuyer.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentCompensationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PaymentCompensationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void createOpenRecord(Long orderId,
                                 String paymentChannel,
                                 String failureReason,
                                 String compensationToken) {
        String sql = "INSERT INTO payment_compensation(order_id, payment_channel, failure_reason, compensation_token, compensation_status, created_at) " +
                "VALUES(?, ?, ?, ?, 'OPEN', NOW())";
        jdbcTemplate.update(sql, orderId, paymentChannel, failureReason, compensationToken);
    }

    public int consumeOpenToken(Long orderId, String compensationToken) {
        String sql = "UPDATE payment_compensation SET compensation_status = 'COMPENSATED', compensated_at = NOW() " +
                "WHERE order_id = ? AND compensation_token = ? AND compensation_status = 'OPEN'";
        return jdbcTemplate.update(sql, orderId, compensationToken);
    }

    public long countFailedPaymentEvents() {
        String sql = "SELECT COUNT(1) FROM payment_compensation";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count.longValue();
    }

    public long countCompensatedPaymentEvents() {
        String sql = "SELECT COUNT(1) FROM payment_compensation WHERE compensation_status = 'COMPENSATED'";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count.longValue();
    }
}
