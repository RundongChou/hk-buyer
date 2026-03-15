package com.hkbuyer.repository;

import com.hkbuyer.domain.OpsAlertEvent;
import com.hkbuyer.domain.OpsExperiment;
import com.hkbuyer.domain.OpsExperimentAssignment;
import com.hkbuyer.domain.OpsVariant;
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
public class OptimizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public OptimizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createExperiment(OpsExperiment item) {
        String sql = "INSERT INTO ops_experiment(experiment_key, experiment_name, experiment_status, control_ratio, treatment_ratio, " +
                "treatment_markup_delta, treatment_sla_hours, created_by, activated_by, activated_at, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, item.getExperimentKey());
            ps.setString(2, item.getExperimentName());
            ps.setString(3, item.getExperimentStatus());
            ps.setBigDecimal(4, item.getControlRatio());
            ps.setBigDecimal(5, item.getTreatmentRatio());
            ps.setBigDecimal(6, item.getTreatmentMarkupDelta());
            ps.setInt(7, item.getTreatmentSlaHours().intValue());
            ps.setLong(8, item.getCreatedBy().longValue());
            if (item.getActivatedBy() == null) {
                ps.setObject(9, null);
            } else {
                ps.setLong(9, item.getActivatedBy().longValue());
            }
            if (item.getActivatedAt() == null) {
                ps.setObject(10, null);
            } else {
                ps.setTimestamp(10, Timestamp.valueOf(item.getActivatedAt()));
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<OpsExperiment> findExperimentById(Long experimentId) {
        String sql = "SELECT experiment_id, experiment_key, experiment_name, experiment_status, control_ratio, treatment_ratio, " +
                "treatment_markup_delta, treatment_sla_hours, created_by, activated_by, activated_at, created_at, updated_at " +
                "FROM ops_experiment WHERE experiment_id = ?";
        List<OpsExperiment> rows = jdbcTemplate.query(sql, experimentRowMapper(), experimentId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<OpsExperiment> findActiveExperiment() {
        String sql = "SELECT experiment_id, experiment_key, experiment_name, experiment_status, control_ratio, treatment_ratio, " +
                "treatment_markup_delta, treatment_sla_hours, created_by, activated_by, activated_at, created_at, updated_at " +
                "FROM ops_experiment WHERE experiment_status = 'ACTIVE' ORDER BY activated_at DESC, experiment_id DESC LIMIT 1";
        List<OpsExperiment> rows = jdbcTemplate.query(sql, experimentRowMapper());
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public int pauseActiveExperiments() {
        String sql = "UPDATE ops_experiment SET experiment_status = 'PAUSED', updated_at = NOW() WHERE experiment_status = 'ACTIVE'";
        return jdbcTemplate.update(sql);
    }

    public int activateExperiment(Long experimentId, Long adminId, LocalDateTime activatedAt) {
        String sql = "UPDATE ops_experiment SET experiment_status = 'ACTIVE', activated_by = ?, activated_at = ?, updated_at = NOW() " +
                "WHERE experiment_id = ?";
        return jdbcTemplate.update(sql,
                adminId,
                Timestamp.valueOf(activatedAt),
                experimentId);
    }

    public Optional<OpsExperimentAssignment> findAssignment(Long experimentId, Long orderId) {
        String sql = "SELECT assignment_id, experiment_id, order_id, user_id, variant, base_sla_hours, final_sla_hours, " +
                "base_markup, final_markup, assigned_at FROM ops_experiment_assignment WHERE experiment_id = ? AND order_id = ?";
        List<OpsExperimentAssignment> rows = jdbcTemplate.query(sql, assignmentRowMapper(), experimentId, orderId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public long createAssignment(OpsExperimentAssignment item) {
        String sql = "INSERT INTO ops_experiment_assignment(experiment_id, order_id, user_id, variant, base_sla_hours, " +
                "final_sla_hours, base_markup, final_markup, assigned_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, item.getExperimentId().longValue());
            ps.setLong(2, item.getOrderId().longValue());
            ps.setLong(3, item.getUserId().longValue());
            ps.setString(4, item.getVariant().name());
            ps.setInt(5, item.getBaseSlaHours().intValue());
            ps.setInt(6, item.getFinalSlaHours().intValue());
            ps.setBigDecimal(7, item.getBaseMarkup());
            ps.setBigDecimal(8, item.getFinalMarkup());
            ps.setTimestamp(9, Timestamp.valueOf(item.getAssignedAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<OpsExperimentAssignment> listAssignmentsByExperiment(Long experimentId, int limit) {
        String sql = "SELECT assignment_id, experiment_id, order_id, user_id, variant, base_sla_hours, final_sla_hours, " +
                "base_markup, final_markup, assigned_at FROM ops_experiment_assignment " +
                "WHERE experiment_id = ? ORDER BY assignment_id DESC LIMIT " + limit;
        return jdbcTemplate.query(sql, assignmentRowMapper(), experimentId);
    }

    public long countExperiments() {
        String sql = "SELECT COUNT(1) FROM ops_experiment";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countActiveExperiments() {
        String sql = "SELECT COUNT(1) FROM ops_experiment WHERE experiment_status = 'ACTIVE'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countAssignments() {
        String sql = "SELECT COUNT(1) FROM ops_experiment_assignment";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countAssignmentsByVariant(OpsVariant variant) {
        String sql = "SELECT COUNT(1) FROM ops_experiment_assignment WHERE variant = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, variant.name());
        return value == null ? 0L : value.longValue();
    }

    public long countSignedAssignmentsByVariant(OpsVariant variant) {
        String sql = "SELECT COUNT(1) FROM ops_experiment_assignment a " +
                "JOIN shipment_tracking s ON s.order_id = a.order_id " +
                "WHERE a.variant = ? AND s.shipment_status = 'SIGNED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, variant.name());
        return value == null ? 0L : value.longValue();
    }

    public long countSignedWithinSevenToFifteenByVariant(OpsVariant variant) {
        String sql = "SELECT COUNT(1) FROM ops_experiment_assignment a " +
                "JOIN shipment_tracking s ON s.order_id = a.order_id " +
                "JOIN order_main o ON o.order_id = a.order_id " +
                "WHERE a.variant = ? AND s.shipment_status = 'SIGNED' AND s.signed_at IS NOT NULL " +
                "AND TIMESTAMPDIFF(DAY, o.created_at, s.signed_at) BETWEEN 7 AND 15";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, variant.name());
        return value == null ? 0L : value.longValue();
    }

    public BigDecimal avgPlatformServiceRateByVariant(OpsVariant variant) {
        String sql = "SELECT COALESCE(AVG(CASE WHEN sl.order_amount > 0 " +
                "THEN sl.platform_service_amount / sl.order_amount ELSE NULL END), 0) " +
                "FROM ops_experiment_assignment a JOIN settlement_ledger sl ON sl.order_id = a.order_id " +
                "WHERE a.variant = ?";
        return queryDecimal(sql, variant.name());
    }

    public void createAlert(String alertType,
                            String metricKey,
                            BigDecimal metricValue,
                            BigDecimal thresholdValue,
                            String severity,
                            String detail,
                            LocalDateTime createdAt) {
        String sql = "INSERT INTO ops_alert_event(alert_type, metric_key, metric_value, threshold_value, severity, alert_status, detail, created_at, resolved_at) " +
                "VALUES(?, ?, ?, ?, ?, 'OPEN', ?, ?, NULL)";
        jdbcTemplate.update(sql,
                alertType,
                metricKey,
                metricValue,
                thresholdValue,
                severity,
                detail,
                Timestamp.valueOf(createdAt));
    }

    public long countOpenAlerts() {
        String sql = "SELECT COUNT(1) FROM ops_alert_event WHERE alert_status = 'OPEN'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countOpenAlertsBySeverity(String severity) {
        String sql = "SELECT COUNT(1) FROM ops_alert_event WHERE alert_status = 'OPEN' AND severity = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, severity);
        return value == null ? 0L : value.longValue();
    }

    public long countOpenAlertsByType(String alertType) {
        String sql = "SELECT COUNT(1) FROM ops_alert_event WHERE alert_status = 'OPEN' AND alert_type = ?";
        Long value = jdbcTemplate.queryForObject(sql, Long.class, alertType);
        return value == null ? 0L : value.longValue();
    }

    public int resolveOpenAlertsByType(String alertType, String detail, LocalDateTime resolvedAt) {
        String sql = "UPDATE ops_alert_event SET alert_status = 'RESOLVED', detail = ?, resolved_at = ? " +
                "WHERE alert_type = ? AND alert_status = 'OPEN'";
        return jdbcTemplate.update(sql,
                detail,
                Timestamp.valueOf(resolvedAt),
                alertType);
    }

    public List<OpsAlertEvent> listOpenAlerts(int limit) {
        String sql = "SELECT alert_id, alert_type, metric_key, metric_value, threshold_value, severity, alert_status, detail, created_at, resolved_at " +
                "FROM ops_alert_event WHERE alert_status = 'OPEN' ORDER BY severity DESC, created_at DESC LIMIT " + limit;
        return jdbcTemplate.query(sql, alertRowMapper());
    }

    private BigDecimal queryDecimal(String sql, Object arg) {
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, arg);
        return value == null ? BigDecimal.ZERO : value;
    }

    private RowMapper<OpsExperiment> experimentRowMapper() {
        return (rs, rowNum) -> {
            OpsExperiment item = new OpsExperiment();
            item.setExperimentId(rs.getLong("experiment_id"));
            item.setExperimentKey(rs.getString("experiment_key"));
            item.setExperimentName(rs.getString("experiment_name"));
            item.setExperimentStatus(rs.getString("experiment_status"));
            item.setControlRatio(rs.getBigDecimal("control_ratio"));
            item.setTreatmentRatio(rs.getBigDecimal("treatment_ratio"));
            item.setTreatmentMarkupDelta(rs.getBigDecimal("treatment_markup_delta"));
            item.setTreatmentSlaHours(Integer.valueOf(rs.getInt("treatment_sla_hours")));
            item.setCreatedBy(rs.getLong("created_by"));
            long activatedBy = rs.getLong("activated_by");
            if (!rs.wasNull()) {
                item.setActivatedBy(Long.valueOf(activatedBy));
            }
            item.setActivatedAt(toLocalDateTime(rs.getTimestamp("activated_at")));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return item;
        };
    }

    private RowMapper<OpsExperimentAssignment> assignmentRowMapper() {
        return (rs, rowNum) -> {
            OpsExperimentAssignment item = new OpsExperimentAssignment();
            item.setAssignmentId(rs.getLong("assignment_id"));
            item.setExperimentId(rs.getLong("experiment_id"));
            item.setOrderId(rs.getLong("order_id"));
            item.setUserId(rs.getLong("user_id"));
            item.setVariant(OpsVariant.valueOf(rs.getString("variant")));
            item.setBaseSlaHours(Integer.valueOf(rs.getInt("base_sla_hours")));
            item.setFinalSlaHours(Integer.valueOf(rs.getInt("final_sla_hours")));
            item.setBaseMarkup(rs.getBigDecimal("base_markup"));
            item.setFinalMarkup(rs.getBigDecimal("final_markup"));
            item.setAssignedAt(toLocalDateTime(rs.getTimestamp("assigned_at")));
            return item;
        };
    }

    private RowMapper<OpsAlertEvent> alertRowMapper() {
        return (rs, rowNum) -> {
            OpsAlertEvent item = new OpsAlertEvent();
            item.setAlertId(rs.getLong("alert_id"));
            item.setAlertType(rs.getString("alert_type"));
            item.setMetricKey(rs.getString("metric_key"));
            item.setMetricValue(rs.getBigDecimal("metric_value"));
            item.setThresholdValue(rs.getBigDecimal("threshold_value"));
            item.setSeverity(rs.getString("severity"));
            item.setAlertStatus(rs.getString("alert_status"));
            item.setDetail(rs.getString("detail"));
            item.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
            item.setResolvedAt(toLocalDateTime(rs.getTimestamp("resolved_at")));
            return item;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
