package com.hkbuyer.repository;

import com.hkbuyer.domain.AfterSaleCase;
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
public class AfterSaleRepository {

    private final JdbcTemplate jdbcTemplate;

    public AfterSaleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createCase(AfterSaleCase item) {
        String sql = "INSERT INTO after_sale_case(order_id, task_id, user_id, buyer_id, case_type, case_status, issue_reason, " +
                "evidence_url, replacement_sku_name, suggested_refund_amount, negotiated_refund_amount, user_decision, user_comment, " +
                "arbitration_result, risk_level, origin_order_status, admin_id, arbitration_comment, user_decision_at, arbitrated_at, " +
                "closed_at, created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getOrderId().longValue());
            if (item.getTaskId() == null) {
                ps.setObject(2, null);
            } else {
                ps.setLong(2, item.getTaskId().longValue());
            }
            ps.setLong(3, item.getUserId().longValue());
            if (item.getBuyerId() == null) {
                ps.setObject(4, null);
            } else {
                ps.setLong(4, item.getBuyerId().longValue());
            }
            ps.setString(5, item.getCaseType());
            ps.setString(6, item.getCaseStatus());
            ps.setString(7, item.getIssueReason());
            ps.setString(8, item.getEvidenceUrl());
            ps.setString(9, item.getReplacementSkuName());
            ps.setBigDecimal(10, item.getSuggestedRefundAmount());
            ps.setBigDecimal(11, item.getNegotiatedRefundAmount());
            ps.setString(12, item.getUserDecision());
            ps.setString(13, item.getUserComment());
            ps.setString(14, item.getArbitrationResult());
            ps.setString(15, item.getRiskLevel());
            ps.setString(16, item.getOriginOrderStatus());
            if (item.getAdminId() == null) {
                ps.setObject(17, null);
            } else {
                ps.setLong(17, item.getAdminId().longValue());
            }
            ps.setString(18, item.getArbitrationComment());
            if (item.getUserDecisionAt() == null) {
                ps.setObject(19, null);
            } else {
                ps.setTimestamp(19, Timestamp.valueOf(item.getUserDecisionAt()));
            }
            if (item.getArbitratedAt() == null) {
                ps.setObject(20, null);
            } else {
                ps.setTimestamp(20, Timestamp.valueOf(item.getArbitratedAt()));
            }
            if (item.getClosedAt() == null) {
                ps.setObject(21, null);
            } else {
                ps.setTimestamp(21, Timestamp.valueOf(item.getClosedAt()));
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<AfterSaleCase> findById(Long caseId) {
        String sql = "SELECT case_id, order_id, task_id, user_id, buyer_id, case_type, case_status, issue_reason, evidence_url, " +
                "replacement_sku_name, suggested_refund_amount, negotiated_refund_amount, user_decision, user_comment, arbitration_result, " +
                "risk_level, origin_order_status, admin_id, arbitration_comment, user_decision_at, arbitrated_at, closed_at, created_at, updated_at " +
                "FROM after_sale_case WHERE case_id = ?";
        List<AfterSaleCase> rows = jdbcTemplate.query(sql, caseRowMapper(), caseId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<AfterSaleCase> listByOrderId(Long orderId) {
        String sql = "SELECT case_id, order_id, task_id, user_id, buyer_id, case_type, case_status, issue_reason, evidence_url, " +
                "replacement_sku_name, suggested_refund_amount, negotiated_refund_amount, user_decision, user_comment, arbitration_result, " +
                "risk_level, origin_order_status, admin_id, arbitration_comment, user_decision_at, arbitrated_at, closed_at, created_at, updated_at " +
                "FROM after_sale_case WHERE order_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, caseRowMapper(), orderId);
    }

    public List<AfterSaleCase> listPendingArbitrationCases() {
        String sql = "SELECT case_id, order_id, task_id, user_id, buyer_id, case_type, case_status, issue_reason, evidence_url, " +
                "replacement_sku_name, suggested_refund_amount, negotiated_refund_amount, user_decision, user_comment, arbitration_result, " +
                "risk_level, origin_order_status, admin_id, arbitration_comment, user_decision_at, arbitrated_at, closed_at, created_at, updated_at " +
                "FROM after_sale_case WHERE case_status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, caseRowMapper(), "ADMIN_REVIEW");
    }

    public void markUserDecision(Long caseId,
                                 String caseStatus,
                                 String userDecision,
                                 String userComment,
                                 LocalDateTime userDecisionAt) {
        String sql = "UPDATE after_sale_case SET case_status = ?, user_decision = ?, user_comment = ?, user_decision_at = ?, " +
                "updated_at = NOW() WHERE case_id = ?";
        jdbcTemplate.update(sql,
                caseStatus,
                userDecision,
                userComment,
                Timestamp.valueOf(userDecisionAt),
                caseId);
    }

    public void markArbitration(Long caseId,
                                String caseStatus,
                                String arbitrationResult,
                                Long adminId,
                                String arbitrationComment,
                                BigDecimal negotiatedRefundAmount,
                                LocalDateTime arbitratedAt,
                                LocalDateTime closedAt) {
        String sql = "UPDATE after_sale_case SET case_status = ?, arbitration_result = ?, admin_id = ?, arbitration_comment = ?, " +
                "negotiated_refund_amount = ?, arbitrated_at = ?, closed_at = ?, updated_at = NOW() WHERE case_id = ?";
        jdbcTemplate.update(sql,
                caseStatus,
                arbitrationResult,
                adminId,
                arbitrationComment,
                negotiatedRefundAmount,
                Timestamp.valueOf(arbitratedAt),
                Timestamp.valueOf(closedAt),
                caseId);
    }

    public long countOpenCasesByOrderId(Long orderId) {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE order_id = ? AND case_status IN ('USER_PENDING', 'ADMIN_REVIEW')";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, orderId);
        return value == null ? 0L : value.longValue();
    }

    public long countOpenStockoutCasesByTaskId(Long taskId) {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE task_id = ? AND case_type = 'STOCKOUT' " +
                "AND case_status IN ('USER_PENDING', 'ADMIN_REVIEW')";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, taskId);
        return value == null ? 0L : value.longValue();
    }

    public long countOpenAuthenticityCasesByOrderId(Long orderId) {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE order_id = ? AND case_type = 'AUTHENTICITY' " +
                "AND case_status IN ('USER_PENDING', 'ADMIN_REVIEW')";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, orderId);
        return value == null ? 0L : value.longValue();
    }

    public long countOpenCases() {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE case_status IN ('USER_PENDING', 'ADMIN_REVIEW')";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countPendingArbitrationCases() {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE case_status = 'ADMIN_REVIEW'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countResolvedCases() {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE case_status = 'RESOLVED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countCounterfeitDisputes() {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE case_type = 'AUTHENTICITY'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countApprovedPartialRefundCases() {
        String sql = "SELECT COUNT(1) FROM after_sale_case WHERE arbitration_result = 'APPROVE_PARTIAL_REFUND'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    private RowMapper<AfterSaleCase> caseRowMapper() {
        return (rs, rowNum) -> {
            AfterSaleCase item = new AfterSaleCase();
            item.setCaseId(rs.getLong("case_id"));
            item.setOrderId(rs.getLong("order_id"));

            Long taskId = rs.getLong("task_id");
            if (!rs.wasNull()) {
                item.setTaskId(taskId);
            }

            item.setUserId(rs.getLong("user_id"));

            Long buyerId = rs.getLong("buyer_id");
            if (!rs.wasNull()) {
                item.setBuyerId(buyerId);
            }

            item.setCaseType(rs.getString("case_type"));
            item.setCaseStatus(rs.getString("case_status"));
            item.setIssueReason(rs.getString("issue_reason"));
            item.setEvidenceUrl(rs.getString("evidence_url"));
            item.setReplacementSkuName(rs.getString("replacement_sku_name"));
            item.setSuggestedRefundAmount(rs.getBigDecimal("suggested_refund_amount"));
            item.setNegotiatedRefundAmount(rs.getBigDecimal("negotiated_refund_amount"));
            item.setUserDecision(rs.getString("user_decision"));
            item.setUserComment(rs.getString("user_comment"));
            item.setArbitrationResult(rs.getString("arbitration_result"));
            item.setRiskLevel(rs.getString("risk_level"));
            item.setOriginOrderStatus(rs.getString("origin_order_status"));

            Long adminId = rs.getLong("admin_id");
            if (!rs.wasNull()) {
                item.setAdminId(adminId);
            }

            item.setArbitrationComment(rs.getString("arbitration_comment"));
            item.setUserDecisionAt(toLocalDateTime(rs.getTimestamp("user_decision_at")));
            item.setArbitratedAt(toLocalDateTime(rs.getTimestamp("arbitrated_at")));
            item.setClosedAt(toLocalDateTime(rs.getTimestamp("closed_at")));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
