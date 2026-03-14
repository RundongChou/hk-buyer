package com.hkbuyer.repository;

import com.hkbuyer.domain.SettlementLedger;
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
public class SettlementRepository {

    private static final String LEDGER_COLUMNS = "ledger_id, order_id, task_id, buyer_id, buyer_settlement_account, order_amount, " +
            "goods_cost_amount, buyer_income_amount, logistics_cost_amount, platform_service_amount, settlement_status, " +
            "reconciliation_status, exception_reason, signed_at, payout_requested_at, settled_at, reconciled_at, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public SettlementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createLedger(SettlementLedger item) {
        String sql = "INSERT INTO settlement_ledger(order_id, task_id, buyer_id, buyer_settlement_account, order_amount, " +
                "goods_cost_amount, buyer_income_amount, logistics_cost_amount, platform_service_amount, settlement_status, " +
                "reconciliation_status, exception_reason, signed_at, payout_requested_at, settled_at, reconciled_at, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getOrderId().longValue());
            ps.setLong(2, item.getTaskId().longValue());
            ps.setLong(3, item.getBuyerId().longValue());
            ps.setString(4, item.getBuyerSettlementAccount());
            ps.setBigDecimal(5, item.getOrderAmount());
            ps.setBigDecimal(6, item.getGoodsCostAmount());
            ps.setBigDecimal(7, item.getBuyerIncomeAmount());
            ps.setBigDecimal(8, item.getLogisticsCostAmount());
            ps.setBigDecimal(9, item.getPlatformServiceAmount());
            ps.setString(10, item.getSettlementStatus());
            ps.setString(11, item.getReconciliationStatus());
            ps.setString(12, item.getExceptionReason());
            ps.setTimestamp(13, item.getSignedAt() == null ? null : Timestamp.valueOf(item.getSignedAt()));
            ps.setTimestamp(14, item.getPayoutRequestedAt() == null ? null : Timestamp.valueOf(item.getPayoutRequestedAt()));
            ps.setTimestamp(15, item.getSettledAt() == null ? null : Timestamp.valueOf(item.getSettledAt()));
            ps.setTimestamp(16, item.getReconciledAt() == null ? null : Timestamp.valueOf(item.getReconciledAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<SettlementLedger> findByOrderId(Long orderId) {
        String sql = "SELECT " + LEDGER_COLUMNS + " FROM settlement_ledger WHERE order_id = ?";
        List<SettlementLedger> rows = jdbcTemplate.query(sql, settlementLedgerRowMapper(), orderId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<SettlementLedger> findById(Long ledgerId) {
        String sql = "SELECT " + LEDGER_COLUMNS + " FROM settlement_ledger WHERE ledger_id = ?";
        List<SettlementLedger> rows = jdbcTemplate.query(sql, settlementLedgerRowMapper(), ledgerId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public List<SettlementLedger> listByBuyerId(Long buyerId, String settlementStatus, int limit) {
        if (settlementStatus == null) {
            String sql = "SELECT " + LEDGER_COLUMNS + " FROM settlement_ledger WHERE buyer_id = ? ORDER BY created_at DESC LIMIT ?";
            return jdbcTemplate.query(sql, settlementLedgerRowMapper(), buyerId, Integer.valueOf(limit));
        }
        String sql = "SELECT " + LEDGER_COLUMNS + " FROM settlement_ledger " +
                "WHERE buyer_id = ? AND settlement_status = ? ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, settlementLedgerRowMapper(), buyerId, settlementStatus, Integer.valueOf(limit));
    }

    public List<SettlementLedger> listPendingPayout(int limit) {
        String sql = "SELECT " + LEDGER_COLUMNS + " FROM settlement_ledger " +
                "WHERE settlement_status = 'PAYOUT_REQUESTED' ORDER BY payout_requested_at ASC, ledger_id ASC LIMIT ?";
        return jdbcTemplate.query(sql, settlementLedgerRowMapper(), Integer.valueOf(limit));
    }

    public int markPayoutRequested(Long ledgerId, LocalDateTime payoutRequestedAt) {
        String sql = "UPDATE settlement_ledger SET settlement_status = 'PAYOUT_REQUESTED', payout_requested_at = ?, " +
                "updated_at = NOW() WHERE ledger_id = ? AND settlement_status = 'PENDING'";
        return jdbcTemplate.update(sql, Timestamp.valueOf(payoutRequestedAt), ledgerId);
    }

    public int markSettled(Long ledgerId, LocalDateTime settledAt) {
        String sql = "UPDATE settlement_ledger SET settlement_status = 'SETTLED', settled_at = ?, updated_at = NOW() " +
                "WHERE ledger_id = ? AND settlement_status = 'PAYOUT_REQUESTED'";
        return jdbcTemplate.update(sql, Timestamp.valueOf(settledAt), ledgerId);
    }

    public int markReconciliation(Long ledgerId,
                                  String reconciliationStatus,
                                  String exceptionReason,
                                  LocalDateTime reconciledAt) {
        String sql = "UPDATE settlement_ledger SET reconciliation_status = ?, exception_reason = ?, reconciled_at = ?, updated_at = NOW() " +
                "WHERE ledger_id = ? AND settlement_status = 'SETTLED'";
        return jdbcTemplate.update(sql,
                reconciliationStatus,
                exceptionReason,
                reconciledAt == null ? null : Timestamp.valueOf(reconciledAt),
                ledgerId);
    }

    public long countTotalLedgers() {
        String sql = "SELECT COUNT(1) FROM settlement_ledger";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countBySettlementStatus(String settlementStatus) {
        String sql = "SELECT COUNT(1) FROM settlement_ledger WHERE settlement_status = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, settlementStatus);
        return value == null ? 0L : value.longValue();
    }

    public long countByReconciliationStatus(String reconciliationStatus) {
        String sql = "SELECT COUNT(1) FROM settlement_ledger WHERE reconciliation_status = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, reconciliationStatus);
        return value == null ? 0L : value.longValue();
    }

    public BigDecimal sumOrderAmount() {
        return queryDecimal("SELECT COALESCE(SUM(order_amount), 0) FROM settlement_ledger");
    }

    public BigDecimal sumPlatformServiceAmount() {
        return queryDecimal("SELECT COALESCE(SUM(platform_service_amount), 0) FROM settlement_ledger");
    }

    private BigDecimal queryDecimal(String sql) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return value == null ? BigDecimal.ZERO : value;
    }

    private RowMapper<SettlementLedger> settlementLedgerRowMapper() {
        return (rs, rowNum) -> {
            SettlementLedger item = new SettlementLedger();
            item.setLedgerId(rs.getLong("ledger_id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setTaskId(rs.getLong("task_id"));
            item.setBuyerId(rs.getLong("buyer_id"));
            item.setBuyerSettlementAccount(rs.getString("buyer_settlement_account"));
            item.setOrderAmount(rs.getBigDecimal("order_amount"));
            item.setGoodsCostAmount(rs.getBigDecimal("goods_cost_amount"));
            item.setBuyerIncomeAmount(rs.getBigDecimal("buyer_income_amount"));
            item.setLogisticsCostAmount(rs.getBigDecimal("logistics_cost_amount"));
            item.setPlatformServiceAmount(rs.getBigDecimal("platform_service_amount"));
            item.setSettlementStatus(rs.getString("settlement_status"));
            item.setReconciliationStatus(rs.getString("reconciliation_status"));
            item.setExceptionReason(rs.getString("exception_reason"));
            item.setSignedAt(toLocalDateTime(rs.getTimestamp("signed_at")));
            item.setPayoutRequestedAt(toLocalDateTime(rs.getTimestamp("payout_requested_at")));
            item.setSettledAt(toLocalDateTime(rs.getTimestamp("settled_at")));
            item.setReconciledAt(toLocalDateTime(rs.getTimestamp("reconciled_at")));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
