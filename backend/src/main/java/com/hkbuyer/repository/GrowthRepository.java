package com.hkbuyer.repository;

import com.hkbuyer.domain.GrowthCampaign;
import com.hkbuyer.domain.MembershipLevel;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GrowthRepository {

    private final JdbcTemplate jdbcTemplate;

    public GrowthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertMembershipProfile(Long userId,
                                        MembershipLevel membershipLevel,
                                        long totalPaidOrders,
                                        BigDecimal totalPaidAmount,
                                        long memberPoints) {
        String sql = "INSERT INTO user_membership_profile(user_id, member_level, total_paid_orders, total_paid_amount, member_points, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE member_level = VALUES(member_level), total_paid_orders = VALUES(total_paid_orders), " +
                "total_paid_amount = VALUES(total_paid_amount), member_points = VALUES(member_points), updated_at = NOW()";
        jdbcTemplate.update(sql,
                userId,
                membershipLevel.name(),
                Long.valueOf(totalPaidOrders),
                totalPaidAmount,
                Long.valueOf(memberPoints));
    }

    public long createCampaign(String campaignName,
                               String targetMemberLevel,
                               String couponCode,
                               String campaignStatus,
                               LocalDateTime startAt,
                               LocalDateTime endAt,
                               Long createdBy) {
        String sql = "INSERT INTO growth_campaign(campaign_name, target_member_level, coupon_code, campaign_status, start_at, end_at, created_by, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, campaignName);
            ps.setString(2, targetMemberLevel);
            ps.setString(3, couponCode);
            ps.setString(4, campaignStatus);
            ps.setTimestamp(5, Timestamp.valueOf(startAt));
            ps.setTimestamp(6, Timestamp.valueOf(endAt));
            ps.setLong(7, createdBy.longValue());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<GrowthCampaign> findCampaignById(Long campaignId) {
        String sql = "SELECT campaign_id, campaign_name, target_member_level, coupon_code, campaign_status, start_at, end_at, created_by, created_at, updated_at " +
                "FROM growth_campaign WHERE campaign_id = ?";
        List<GrowthCampaign> rows = jdbcTemplate.query(sql, growthCampaignRowMapper(), campaignId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<GrowthCampaign> findActiveCampaignByCouponCode(String couponCode, LocalDateTime now) {
        String sql = "SELECT campaign_id, campaign_name, target_member_level, coupon_code, campaign_status, start_at, end_at, created_by, created_at, updated_at " +
                "FROM growth_campaign WHERE coupon_code = ? AND campaign_status = 'ACTIVE' AND start_at <= ? AND end_at >= ? " +
                "ORDER BY updated_at DESC LIMIT 1";
        Timestamp nowTs = Timestamp.valueOf(now);
        List<GrowthCampaign> rows = jdbcTemplate.query(sql, growthCampaignRowMapper(), couponCode, nowTs, nowTs);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<GrowthCampaign> listCampaigns(int limit) {
        String sql = "SELECT campaign_id, campaign_name, target_member_level, coupon_code, campaign_status, start_at, end_at, created_by, created_at, updated_at " +
                "FROM growth_campaign ORDER BY updated_at DESC, campaign_id DESC LIMIT " + limit;
        return jdbcTemplate.query(sql, growthCampaignRowMapper());
    }

    public int updateCampaignStatus(Long campaignId, String campaignStatus) {
        String sql = "UPDATE growth_campaign SET campaign_status = ?, updated_at = NOW() WHERE campaign_id = ?";
        return jdbcTemplate.update(sql, campaignStatus, campaignId);
    }

    public int upsertTouch(Long campaignId,
                           Long userId,
                           String touchChannel,
                           String touchStatus,
                           String couponCode,
                           LocalDateTime touchedAt) {
        String sql = "INSERT INTO growth_campaign_touch(campaign_id, user_id, touch_channel, touch_status, coupon_code, touched_at, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE touch_channel = VALUES(touch_channel), touch_status = VALUES(touch_status), " +
                "coupon_code = VALUES(coupon_code), touched_at = VALUES(touched_at), updated_at = NOW()";
        return jdbcTemplate.update(sql,
                campaignId,
                userId,
                touchChannel,
                touchStatus,
                couponCode,
                Timestamp.valueOf(touchedAt));
    }

    public List<Map<String, Object>> listUserTouches(Long userId, int limit) {
        String sql = "SELECT t.touch_id, t.campaign_id, c.campaign_name, c.target_member_level, c.campaign_status, " +
                "t.user_id, t.touch_channel, t.touch_status, t.coupon_code, t.touched_at, c.start_at, c.end_at " +
                "FROM growth_campaign_touch t JOIN growth_campaign c ON c.campaign_id = t.campaign_id " +
                "WHERE t.user_id = ? ORDER BY t.touched_at DESC LIMIT " + limit;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("touchId", rs.getLong("touch_id"));
            payload.put("campaignId", rs.getLong("campaign_id"));
            payload.put("campaignName", rs.getString("campaign_name"));
            payload.put("targetMemberLevel", rs.getString("target_member_level"));
            payload.put("campaignStatus", rs.getString("campaign_status"));
            payload.put("userId", rs.getLong("user_id"));
            payload.put("touchChannel", rs.getString("touch_channel"));
            payload.put("touchStatus", rs.getString("touch_status"));
            payload.put("couponCode", rs.getString("coupon_code"));
            Timestamp touchedAt = rs.getTimestamp("touched_at");
            payload.put("touchedAt", touchedAt == null ? null : touchedAt.toLocalDateTime());
            Timestamp startAt = rs.getTimestamp("start_at");
            payload.put("startAt", startAt == null ? null : startAt.toLocalDateTime());
            Timestamp endAt = rs.getTimestamp("end_at");
            payload.put("endAt", endAt == null ? null : endAt.toLocalDateTime());
            return payload;
        }, userId);
    }

    public long countMembershipProfiles() {
        String sql = "SELECT COUNT(1) FROM user_membership_profile";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countMembershipProfilesByLevel(MembershipLevel membershipLevel) {
        String sql = "SELECT COUNT(1) FROM user_membership_profile WHERE member_level = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, membershipLevel.name());
        return value == null ? 0L : value.longValue();
    }

    public long countCampaigns() {
        String sql = "SELECT COUNT(1) FROM growth_campaign";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countActiveCampaigns(LocalDateTime now) {
        String sql = "SELECT COUNT(1) FROM growth_campaign WHERE campaign_status = 'ACTIVE' AND start_at <= ? AND end_at >= ?";
        Timestamp nowTs = Timestamp.valueOf(now);
        Long value = jdbcTemplate.queryForObject(sql, Long.class, nowTs, nowTs);
        return value == null ? 0L : value.longValue();
    }

    public long countTouchesByStatus(String touchStatus) {
        String sql = "SELECT COUNT(1) FROM growth_campaign_touch WHERE touch_status = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, touchStatus);
        return value == null ? 0L : value.longValue();
    }

    public long countDistinctTouchedUsers() {
        String sql = "SELECT COUNT(DISTINCT user_id) FROM growth_campaign_touch";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countPaidOrdersWithCouponAttribution() {
        String sql = "SELECT COUNT(1) FROM order_main WHERE pay_status = 'PAID' AND applied_coupon_code IS NOT NULL";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    private RowMapper<GrowthCampaign> growthCampaignRowMapper() {
        return (rs, rowNum) -> {
            GrowthCampaign campaign = new GrowthCampaign();
            campaign.setCampaignId(rs.getLong("campaign_id"));
            campaign.setCampaignName(rs.getString("campaign_name"));
            campaign.setTargetMemberLevel(rs.getString("target_member_level"));
            campaign.setCouponCode(rs.getString("coupon_code"));
            campaign.setCampaignStatus(rs.getString("campaign_status"));

            Timestamp startAt = rs.getTimestamp("start_at");
            campaign.setStartAt(startAt == null ? null : startAt.toLocalDateTime());
            Timestamp endAt = rs.getTimestamp("end_at");
            campaign.setEndAt(endAt == null ? null : endAt.toLocalDateTime());

            campaign.setCreatedBy(rs.getLong("created_by"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            campaign.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            campaign.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return campaign;
        };
    }
}
