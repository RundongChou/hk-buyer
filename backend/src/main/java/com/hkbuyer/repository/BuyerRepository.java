package com.hkbuyer.repository;

import com.hkbuyer.domain.BuyerAuditStatus;
import com.hkbuyer.domain.BuyerLevel;
import com.hkbuyer.domain.BuyerOnboardingApplication;
import com.hkbuyer.domain.BuyerProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class BuyerRepository {

    private static final int DEFAULT_CREDIT_SCORE = 60;

    private final JdbcTemplate jdbcTemplate;

    public BuyerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createOnboardingApplication(Long buyerId,
                                            String realName,
                                            String idCardSuffix,
                                            String serviceRegion,
                                            String specialtyCategory,
                                            String settlementAccount) {
        String sql = "INSERT INTO buyer_onboarding_application(" +
                "buyer_id, real_name, id_card_suffix, service_region, specialty_category, settlement_account, " +
                "application_status, reviewed_by, review_comment, created_at, reviewed_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, NULL, NULL, NOW(), NULL, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, buyerId.longValue());
            ps.setString(2, realName);
            ps.setString(3, idCardSuffix);
            ps.setString(4, serviceRegion);
            ps.setString(5, specialtyCategory);
            ps.setString(6, settlementAccount);
            ps.setString(7, BuyerAuditStatus.PENDING.name());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<BuyerOnboardingApplication> findApplicationById(Long applicationId) {
        String sql = "SELECT application_id, buyer_id, real_name, id_card_suffix, service_region, specialty_category, settlement_account, " +
                "application_status, reviewed_by, review_comment, created_at, reviewed_at, updated_at " +
                "FROM buyer_onboarding_application WHERE application_id = ?";
        List<BuyerOnboardingApplication> rows = jdbcTemplate.query(sql, onboardingRowMapper(), applicationId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<BuyerOnboardingApplication> listPendingApplications(int limit) {
        String sql = "SELECT application_id, buyer_id, real_name, id_card_suffix, service_region, specialty_category, settlement_account, " +
                "application_status, reviewed_by, review_comment, created_at, reviewed_at, updated_at " +
                "FROM buyer_onboarding_application WHERE application_status = ? ORDER BY created_at ASC LIMIT " + limit;
        return jdbcTemplate.query(sql, onboardingRowMapper(), BuyerAuditStatus.PENDING.name());
    }

    public int auditApplication(Long applicationId, BuyerAuditStatus status, Long adminId, String comment) {
        String sql = "UPDATE buyer_onboarding_application SET application_status = ?, reviewed_by = ?, review_comment = ?, reviewed_at = NOW(), updated_at = NOW() " +
                "WHERE application_id = ? AND application_status = ?";
        return jdbcTemplate.update(sql, status.name(), adminId, comment, applicationId, BuyerAuditStatus.PENDING.name());
    }

    public void upsertApprovedProfile(BuyerOnboardingApplication application) {
        String sql = "INSERT INTO buyer_profile(" +
                "buyer_id, display_name, audit_status, buyer_level, credit_score, reward_points, penalty_points, accepted_task_count, " +
                "approved_proof_count, rejected_proof_count, service_region, specialty_category, settlement_account, " +
                "last_active_at, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, 0, 0, 0, 0, 0, ?, ?, ?, NOW(), NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE " +
                "display_name = VALUES(display_name), " +
                "audit_status = VALUES(audit_status), " +
                "service_region = VALUES(service_region), " +
                "specialty_category = VALUES(specialty_category), " +
                "settlement_account = VALUES(settlement_account), " +
                "updated_at = NOW()";
        jdbcTemplate.update(sql,
                application.getBuyerId(),
                application.getRealName(),
                BuyerAuditStatus.APPROVED.name(),
                BuyerLevel.BRONZE.name(),
                Integer.valueOf(DEFAULT_CREDIT_SCORE),
                application.getServiceRegion(),
                application.getSpecialtyCategory(),
                application.getSettlementAccount());
    }

    public void markProfileRejected(Long buyerId) {
        String sql = "UPDATE buyer_profile SET audit_status = ?, updated_at = NOW() WHERE buyer_id = ?";
        jdbcTemplate.update(sql, BuyerAuditStatus.REJECTED.name(), buyerId);
    }

    public Optional<BuyerProfile> findProfileByBuyerId(Long buyerId) {
        String sql = "SELECT buyer_id, display_name, audit_status, buyer_level, credit_score, reward_points, penalty_points, accepted_task_count, " +
                "approved_proof_count, rejected_proof_count, service_region, specialty_category, settlement_account, " +
                "last_active_at, created_at, updated_at " +
                "FROM buyer_profile WHERE buyer_id = ?";
        List<BuyerProfile> rows = jdbcTemplate.query(sql, buyerProfileRowMapper(), buyerId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public void markAcceptedTask(Long buyerId) {
        String sql = "UPDATE buyer_profile SET accepted_task_count = accepted_task_count + 1, last_active_at = NOW(), updated_at = NOW() WHERE buyer_id = ?";
        jdbcTemplate.update(sql, buyerId);
    }

    public void updateReputationAfterProofAudit(Long buyerId, boolean approved) {
        Optional<BuyerProfile> optional = findProfileByBuyerId(buyerId);
        if (!optional.isPresent()) {
            return;
        }

        BuyerProfile profile = optional.get();
        int currentScore = profile.getCreditScore() == null ? DEFAULT_CREDIT_SCORE : profile.getCreditScore().intValue();
        int nextScore = approved ? Math.min(100, currentScore + 5) : Math.max(0, currentScore - 8);
        BuyerLevel nextLevel = BuyerLevel.fromCreditScore(nextScore);

        String sql = "UPDATE buyer_profile SET credit_score = ?, buyer_level = ?, reward_points = reward_points + ?, penalty_points = penalty_points + ?, " +
                "approved_proof_count = approved_proof_count + ?, rejected_proof_count = rejected_proof_count + ?, " +
                "last_active_at = NOW(), updated_at = NOW() WHERE buyer_id = ?";
        jdbcTemplate.update(sql,
                Integer.valueOf(nextScore),
                nextLevel.name(),
                Integer.valueOf(approved ? 2 : 0),
                Integer.valueOf(approved ? 0 : 3),
                Integer.valueOf(approved ? 1 : 0),
                Integer.valueOf(approved ? 0 : 1),
                buyerId);
    }

    public long countPendingApplications() {
        String sql = "SELECT COUNT(1) FROM buyer_onboarding_application WHERE application_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, BuyerAuditStatus.PENDING.name());
        return count == null ? 0L : count.longValue();
    }

    public long countApprovedProfiles() {
        String sql = "SELECT COUNT(1) FROM buyer_profile WHERE audit_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, BuyerAuditStatus.APPROVED.name());
        return count == null ? 0L : count.longValue();
    }

    public long countProfilesByLevel(BuyerLevel buyerLevel) {
        String sql = "SELECT COUNT(1) FROM buyer_profile WHERE audit_status = ? AND buyer_level = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, BuyerAuditStatus.APPROVED.name(), buyerLevel.name());
        return count == null ? 0L : count.longValue();
    }

    private RowMapper<BuyerOnboardingApplication> onboardingRowMapper() {
        return (rs, rowNum) -> {
            BuyerOnboardingApplication item = new BuyerOnboardingApplication();
            item.setApplicationId(rs.getLong("application_id"));
            item.setBuyerId(rs.getLong("buyer_id"));
            item.setRealName(rs.getString("real_name"));
            item.setIdCardSuffix(rs.getString("id_card_suffix"));
            item.setServiceRegion(rs.getString("service_region"));
            item.setSpecialtyCategory(rs.getString("specialty_category"));
            item.setSettlementAccount(rs.getString("settlement_account"));
            item.setApplicationStatus(BuyerAuditStatus.valueOf(rs.getString("application_status")));
            Long reviewedBy = rs.getLong("reviewed_by");
            if (!rs.wasNull()) {
                item.setReviewedBy(reviewedBy);
            }
            item.setReviewComment(rs.getString("review_comment"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp reviewedAt = rs.getTimestamp("reviewed_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            item.setReviewedAt(reviewedAt == null ? null : reviewedAt.toLocalDateTime());
            item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return item;
        };
    }

    private RowMapper<BuyerProfile> buyerProfileRowMapper() {
        return (rs, rowNum) -> {
            BuyerProfile item = new BuyerProfile();
            item.setBuyerId(rs.getLong("buyer_id"));
            item.setDisplayName(rs.getString("display_name"));
            item.setAuditStatus(BuyerAuditStatus.valueOf(rs.getString("audit_status")));
            item.setBuyerLevel(BuyerLevel.valueOf(rs.getString("buyer_level")));
            item.setCreditScore(Integer.valueOf(rs.getInt("credit_score")));
            item.setRewardPoints(Integer.valueOf(rs.getInt("reward_points")));
            item.setPenaltyPoints(Integer.valueOf(rs.getInt("penalty_points")));
            item.setAcceptedTaskCount(Integer.valueOf(rs.getInt("accepted_task_count")));
            item.setApprovedProofCount(Integer.valueOf(rs.getInt("approved_proof_count")));
            item.setRejectedProofCount(Integer.valueOf(rs.getInt("rejected_proof_count")));
            item.setServiceRegion(rs.getString("service_region"));
            item.setSpecialtyCategory(rs.getString("specialty_category"));
            item.setSettlementAccount(rs.getString("settlement_account"));
            Timestamp lastActiveAt = rs.getTimestamp("last_active_at");
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            item.setLastActiveAt(lastActiveAt == null ? null : lastActiveAt.toLocalDateTime());
            item.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            item.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return item;
        };
    }
}
