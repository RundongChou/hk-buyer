package com.hkbuyer.repository;

import com.hkbuyer.domain.BuyerLevel;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.domain.TaskTier;
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
public class TaskRepository {

    private final JdbcTemplate jdbcTemplate;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createTask(Long orderId,
                           BigDecimal suggestedMarkup,
                           LocalDateTime acceptDeadline,
                           TaskTier taskTier,
                           BuyerLevel requiredBuyerLevel,
                           String targetRegion,
                           String targetCategory,
                           Integer slaHours) {
        String sql = "INSERT INTO procurement_task(order_id, buyer_id, task_status, publish_at, accept_deadline, suggested_markup, " +
                "task_tier, required_buyer_level, target_region, target_category, sla_hours, markup_applied_count, redispatch_count, " +
                "last_markup_at, next_markup_eligible_at, terminal_reason, accepted_at, updated_at) " +
                "VALUES(?, NULL, ?, NOW(), ?, ?, ?, ?, ?, ?, ?, 0, 0, NULL, NULL, NULL, NULL, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, orderId.longValue());
            ps.setString(2, TaskStatus.PUBLISHED.name());
            ps.setTimestamp(3, Timestamp.valueOf(acceptDeadline));
            ps.setBigDecimal(4, suggestedMarkup);
            ps.setString(5, taskTier.name());
            ps.setString(6, requiredBuyerLevel.name());
            ps.setString(7, targetRegion);
            ps.setString(8, targetCategory);
            ps.setInt(9, slaHours.intValue());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<ProcurementTask> findById(Long taskId) {
        String sql = "SELECT task_id, order_id, buyer_id, task_status, publish_at, accept_deadline, suggested_markup, " +
                "task_tier, required_buyer_level, target_region, target_category, sla_hours, markup_applied_count, redispatch_count, " +
                "last_markup_at, next_markup_eligible_at, terminal_reason, accepted_at, updated_at " +
                "FROM procurement_task WHERE task_id = ?";
        List<ProcurementTask> tasks = jdbcTemplate.query(sql, taskRowMapper(), taskId);
        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks.get(0));
    }

    public Optional<ProcurementTask> findByOrderId(Long orderId) {
        String sql = "SELECT task_id, order_id, buyer_id, task_status, publish_at, accept_deadline, suggested_markup, " +
                "task_tier, required_buyer_level, target_region, target_category, sla_hours, markup_applied_count, redispatch_count, " +
                "last_markup_at, next_markup_eligible_at, terminal_reason, accepted_at, updated_at " +
                "FROM procurement_task WHERE order_id = ? ORDER BY task_id DESC LIMIT 1";
        List<ProcurementTask> tasks = jdbcTemplate.query(sql, taskRowMapper(), orderId);
        if (tasks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tasks.get(0));
    }

    public List<ProcurementTask> listPublishedTasks() {
        String sql = "SELECT task_id, order_id, buyer_id, task_status, publish_at, accept_deadline, suggested_markup, " +
                "task_tier, required_buyer_level, target_region, target_category, sla_hours, markup_applied_count, redispatch_count, " +
                "last_markup_at, next_markup_eligible_at, terminal_reason, accepted_at, updated_at " +
                "FROM procurement_task WHERE task_status = ? ORDER BY publish_at ASC";
        return jdbcTemplate.query(sql, taskRowMapper(), TaskStatus.PUBLISHED.name());
    }

    public List<ProcurementTask> listTimeoutCandidates(int limit) {
        String sql = "SELECT task_id, order_id, buyer_id, task_status, publish_at, accept_deadline, suggested_markup, " +
                "task_tier, required_buyer_level, target_region, target_category, sla_hours, markup_applied_count, redispatch_count, " +
                "last_markup_at, next_markup_eligible_at, terminal_reason, accepted_at, updated_at " +
                "FROM procurement_task WHERE task_status = ? AND accept_deadline < NOW() ORDER BY accept_deadline ASC LIMIT ?";
        return jdbcTemplate.query(sql, taskRowMapper(), TaskStatus.PUBLISHED.name(), Integer.valueOf(limit));
    }

    public int acceptTask(Long taskId, Long buyerId) {
        String sql = "UPDATE procurement_task SET task_status = ?, buyer_id = ?, accepted_at = NOW(), updated_at = NOW() " +
                "WHERE task_id = ? AND task_status = ?";
        return jdbcTemplate.update(sql, TaskStatus.ACCEPTED.name(), buyerId, taskId, TaskStatus.PUBLISHED.name());
    }

    public void markProofSubmitted(Long taskId) {
        String sql = "UPDATE procurement_task SET task_status = ?, updated_at = NOW() WHERE task_id = ?";
        jdbcTemplate.update(sql, TaskStatus.PROOF_SUBMITTED.name(), taskId);
    }

    public void markAuditResult(Long taskId, TaskStatus taskStatus) {
        String sql = "UPDATE procurement_task SET task_status = ?, updated_at = NOW() WHERE task_id = ?";
        jdbcTemplate.update(sql, taskStatus.name(), taskId);
    }

    public int applyTimeoutMarkupAndRedispatch(Long taskId,
                                               BigDecimal suggestedMarkup,
                                               LocalDateTime nextMarkupEligibleAt,
                                               LocalDateTime acceptDeadline) {
        String sql = "UPDATE procurement_task SET suggested_markup = ?, markup_applied_count = markup_applied_count + 1, " +
                "redispatch_count = redispatch_count + 1, last_markup_at = NOW(), next_markup_eligible_at = ?, accept_deadline = ?, " +
                "terminal_reason = NULL, updated_at = NOW() WHERE task_id = ? AND task_status = ?";
        return jdbcTemplate.update(sql,
                suggestedMarkup,
                Timestamp.valueOf(nextMarkupEligibleAt),
                Timestamp.valueOf(acceptDeadline),
                taskId,
                TaskStatus.PUBLISHED.name());
    }

    public int markTimeoutTaskExpired(Long taskId, String terminalReason) {
        String sql = "UPDATE procurement_task SET task_status = ?, terminal_reason = ?, updated_at = NOW() " +
                "WHERE task_id = ? AND task_status = ?";
        return jdbcTemplate.update(sql, TaskStatus.EXPIRED.name(), terminalReason, taskId, TaskStatus.PUBLISHED.name());
    }

    public Long findOrderIdByTaskId(Long taskId) {
        String sql = "SELECT order_id FROM procurement_task WHERE task_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, taskId);
    }

    public long countAcceptedTasks() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE task_status <> ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, TaskStatus.PUBLISHED.name());
        return result == null ? 0L : result.longValue();
    }

    public long countTimeoutUnacceptedTasks() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE task_status = ? AND accept_deadline < NOW()";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, TaskStatus.PUBLISHED.name());
        return result == null ? 0L : result.longValue();
    }

    public long countAllTasks() {
        String sql = "SELECT COUNT(1) FROM procurement_task";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long countTimeoutCandidates() {
        return countTimeoutUnacceptedTasks();
    }

    public long countFrequencyLimitedTimeoutCandidates() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE task_status = ? AND accept_deadline < NOW() " +
                "AND next_markup_eligible_at IS NOT NULL AND next_markup_eligible_at > NOW()";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, TaskStatus.PUBLISHED.name());
        return result == null ? 0L : result.longValue();
    }

    public long countTasksWithAutoMarkup() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE markup_applied_count > 0";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long sumAutoMarkupAppliedCount() {
        String sql = "SELECT COALESCE(SUM(markup_applied_count), 0) FROM procurement_task";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long sumRedispatchCount() {
        String sql = "SELECT COALESCE(SUM(redispatch_count), 0) FROM procurement_task";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    public long countTimeoutTerminatedTasks() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE task_status = ? AND terminal_reason IS NOT NULL";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, TaskStatus.EXPIRED.name());
        return result == null ? 0L : result.longValue();
    }

    public long countAcceptedAfterAutoMarkup() {
        String sql = "SELECT COUNT(1) FROM procurement_task WHERE markup_applied_count > 0 AND task_status IN (?, ?, ?, ?)";
        Long result = jdbcTemplate.queryForObject(sql,
                Long.class,
                TaskStatus.ACCEPTED.name(),
                TaskStatus.PROOF_SUBMITTED.name(),
                TaskStatus.PROOF_APPROVED.name(),
                TaskStatus.PROOF_REJECTED.name());
        return result == null ? 0L : result.longValue();
    }

    private RowMapper<ProcurementTask> taskRowMapper() {
        return (rs, rowNum) -> {
            ProcurementTask task = new ProcurementTask();
            task.setTaskId(rs.getLong("task_id"));
            task.setOrderId(rs.getLong("order_id"));
            Long buyerId = rs.getLong("buyer_id");
            if (!rs.wasNull()) {
                task.setBuyerId(buyerId);
            }
            task.setTaskStatus(TaskStatus.valueOf(rs.getString("task_status")));
            Timestamp publishAt = rs.getTimestamp("publish_at");
            Timestamp acceptDeadline = rs.getTimestamp("accept_deadline");
            Timestamp acceptedAt = rs.getTimestamp("accepted_at");
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            task.setPublishAt(publishAt == null ? null : publishAt.toLocalDateTime());
            task.setAcceptDeadline(acceptDeadline == null ? null : acceptDeadline.toLocalDateTime());
            task.setSuggestedMarkup(rs.getBigDecimal("suggested_markup"));
            task.setTaskTier(TaskTier.valueOf(rs.getString("task_tier")));
            task.setRequiredBuyerLevel(BuyerLevel.valueOf(rs.getString("required_buyer_level")));
            task.setTargetRegion(rs.getString("target_region"));
            task.setTargetCategory(rs.getString("target_category"));
            task.setSlaHours(Integer.valueOf(rs.getInt("sla_hours")));
            task.setMarkupAppliedCount(Integer.valueOf(rs.getInt("markup_applied_count")));
            task.setRedispatchCount(Integer.valueOf(rs.getInt("redispatch_count")));
            Timestamp lastMarkupAt = rs.getTimestamp("last_markup_at");
            Timestamp nextMarkupEligibleAt = rs.getTimestamp("next_markup_eligible_at");
            task.setLastMarkupAt(lastMarkupAt == null ? null : lastMarkupAt.toLocalDateTime());
            task.setNextMarkupEligibleAt(nextMarkupEligibleAt == null ? null : nextMarkupEligibleAt.toLocalDateTime());
            task.setTerminalReason(rs.getString("terminal_reason"));
            task.setAcceptedAt(acceptedAt == null ? null : acceptedAt.toLocalDateTime());
            task.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return task;
        };
    }
}
