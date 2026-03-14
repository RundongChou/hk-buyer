package com.hkbuyer.repository;

import com.hkbuyer.domain.CouponTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class CouponRepository {

    private final JdbcTemplate jdbcTemplate;

    public CouponRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CouponTemplate> findActiveCouponByCode(String couponCode, LocalDateTime now) {
        String sql = "SELECT coupon_id, coupon_code, coupon_name, discount_type, discount_value, min_order_amount, status, valid_from, valid_to " +
                "FROM coupon_template WHERE coupon_code = ? AND status = 'ACTIVE' AND valid_from <= ? AND valid_to >= ? LIMIT 1";
        List<CouponTemplate> rows = jdbcTemplate.query(sql, couponRowMapper(), couponCode, Timestamp.valueOf(now), Timestamp.valueOf(now));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    private RowMapper<CouponTemplate> couponRowMapper() {
        return (rs, rowNum) -> {
            CouponTemplate coupon = new CouponTemplate();
            coupon.setCouponId(rs.getLong("coupon_id"));
            coupon.setCouponCode(rs.getString("coupon_code"));
            coupon.setCouponName(rs.getString("coupon_name"));
            coupon.setDiscountType(rs.getString("discount_type"));
            coupon.setDiscountValue(rs.getBigDecimal("discount_value"));
            coupon.setMinOrderAmount(rs.getBigDecimal("min_order_amount"));
            coupon.setStatus(rs.getString("status"));
            Timestamp validFrom = rs.getTimestamp("valid_from");
            Timestamp validTo = rs.getTimestamp("valid_to");
            coupon.setValidFrom(validFrom == null ? null : validFrom.toLocalDateTime());
            coupon.setValidTo(validTo == null ? null : validTo.toLocalDateTime());
            return coupon;
        };
    }
}
