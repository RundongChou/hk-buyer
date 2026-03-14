package com.hkbuyer.repository;

import com.hkbuyer.domain.CustomsClearanceRecord;
import com.hkbuyer.domain.ShipmentTrackingRecord;
import com.hkbuyer.domain.WarehouseInboundRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class FulfillmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public FulfillmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertBuyerHandover(Long orderId,
                                    Long taskId,
                                    Long buyerId,
                                    String warehouseCode,
                                    LocalDateTime handoverAt) {
        String sql = "INSERT INTO warehouse_inbound(order_id, task_id, buyer_id, warehouse_code, handover_at, " +
                "warehouse_status, qc_status, qc_note, scanned_at, inspected_at, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, 'PENDING_SCAN', 'PENDING', NULL, NULL, NULL, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE task_id = VALUES(task_id), buyer_id = VALUES(buyer_id), warehouse_code = VALUES(warehouse_code), " +
                "handover_at = VALUES(handover_at), warehouse_status = 'PENDING_SCAN', qc_status = 'PENDING', qc_note = NULL, " +
                "scanned_at = NULL, inspected_at = NULL, updated_at = NOW()";
        jdbcTemplate.update(sql,
                orderId,
                taskId,
                buyerId,
                warehouseCode,
                Timestamp.valueOf(handoverAt));
    }

    public int markInboundScan(Long orderId,
                               String warehouseCode,
                               String warehouseStatus,
                               String qcStatus,
                               String qcNote,
                               LocalDateTime scannedAt,
                               LocalDateTime inspectedAt) {
        String sql = "UPDATE warehouse_inbound SET warehouse_code = ?, warehouse_status = ?, qc_status = ?, qc_note = ?, " +
                "scanned_at = ?, inspected_at = ?, updated_at = NOW() WHERE order_id = ?";
        return jdbcTemplate.update(sql,
                warehouseCode,
                warehouseStatus,
                qcStatus,
                qcNote,
                scannedAt == null ? null : Timestamp.valueOf(scannedAt),
                inspectedAt == null ? null : Timestamp.valueOf(inspectedAt),
                orderId);
    }

    public void upsertCustomsSubmission(Long orderId,
                                        String declarationNo,
                                        String complianceChannel,
                                        LocalDateTime submittedAt) {
        String sql = "INSERT INTO customs_clearance_record(order_id, declaration_no, clearance_status, compliance_channel, review_comment, " +
                "submitted_at, reviewed_at, released_at, created_at, updated_at) " +
                "VALUES(?, ?, 'SUBMITTED', ?, NULL, ?, NULL, NULL, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE declaration_no = VALUES(declaration_no), compliance_channel = VALUES(compliance_channel), " +
                "clearance_status = 'SUBMITTED', review_comment = NULL, submitted_at = VALUES(submitted_at), " +
                "reviewed_at = NULL, released_at = NULL, updated_at = NOW()";
        jdbcTemplate.update(sql,
                orderId,
                declarationNo,
                complianceChannel,
                Timestamp.valueOf(submittedAt));
    }

    public int reviewCustoms(Long orderId,
                             String clearanceStatus,
                             String reviewComment,
                             LocalDateTime reviewedAt,
                             LocalDateTime releasedAt) {
        String sql = "UPDATE customs_clearance_record SET clearance_status = ?, review_comment = ?, reviewed_at = ?, released_at = ?, " +
                "updated_at = NOW() WHERE order_id = ?";
        return jdbcTemplate.update(sql,
                clearanceStatus,
                reviewComment,
                reviewedAt == null ? null : Timestamp.valueOf(reviewedAt),
                releasedAt == null ? null : Timestamp.valueOf(releasedAt),
                orderId);
    }

    public void upsertShipment(Long orderId,
                               String carrier,
                               String trackingNo,
                               String shipmentStatus,
                               String latestNode,
                               LocalDateTime latestNodeAt,
                               LocalDateTime signedAt) {
        String sql = "INSERT INTO shipment_tracking(order_id, carrier, tracking_no, shipment_status, latest_node, latest_node_at, signed_at, " +
                "created_at, updated_at) VALUES(?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) " +
                "ON DUPLICATE KEY UPDATE carrier = VALUES(carrier), tracking_no = VALUES(tracking_no), shipment_status = VALUES(shipment_status), " +
                "latest_node = VALUES(latest_node), latest_node_at = VALUES(latest_node_at), signed_at = VALUES(signed_at), updated_at = NOW()";
        jdbcTemplate.update(sql,
                orderId,
                carrier,
                trackingNo,
                shipmentStatus,
                latestNode,
                Timestamp.valueOf(latestNodeAt),
                signedAt == null ? null : Timestamp.valueOf(signedAt));
    }

    public Optional<WarehouseInboundRecord> findInboundByOrderId(Long orderId) {
        String sql = "SELECT inbound_id, order_id, task_id, buyer_id, warehouse_code, handover_at, warehouse_status, qc_status, qc_note, " +
                "scanned_at, inspected_at, updated_at FROM warehouse_inbound WHERE order_id = ?";
        List<WarehouseInboundRecord> rows = jdbcTemplate.query(sql, warehouseRowMapper(), orderId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<CustomsClearanceRecord> findCustomsByOrderId(Long orderId) {
        String sql = "SELECT clearance_id, order_id, declaration_no, clearance_status, compliance_channel, review_comment, submitted_at, " +
                "reviewed_at, released_at, updated_at FROM customs_clearance_record WHERE order_id = ?";
        List<CustomsClearanceRecord> rows = jdbcTemplate.query(sql, customsRowMapper(), orderId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public Optional<ShipmentTrackingRecord> findShipmentByOrderId(Long orderId) {
        String sql = "SELECT shipment_id, order_id, carrier, tracking_no, shipment_status, latest_node, latest_node_at, signed_at, updated_at " +
                "FROM shipment_tracking WHERE order_id = ?";
        List<ShipmentTrackingRecord> rows = jdbcTemplate.query(sql, shipmentRowMapper(), orderId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    public long countInboundCompleted() {
        String sql = "SELECT COUNT(1) FROM warehouse_inbound WHERE warehouse_status = 'INBOUND_COMPLETED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countCustomsSubmitted() {
        String sql = "SELECT COUNT(1) FROM customs_clearance_record";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countCustomsReleased() {
        String sql = "SELECT COUNT(1) FROM customs_clearance_record WHERE clearance_status = 'RELEASED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countCustomsRejected() {
        String sql = "SELECT COUNT(1) FROM customs_clearance_record WHERE clearance_status = 'REJECTED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countShipmentInTransit() {
        String sql = "SELECT COUNT(1) FROM shipment_tracking WHERE shipment_status = 'IN_TRANSIT'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countShipmentSigned() {
        String sql = "SELECT COUNT(1) FROM shipment_tracking WHERE shipment_status = 'SIGNED'";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    public long countSignedWithinSevenToFifteenDays() {
        String sql = "SELECT COUNT(1) FROM order_main o " +
                "INNER JOIN shipment_tracking s ON s.order_id = o.order_id " +
                "WHERE s.signed_at IS NOT NULL " +
                "AND TIMESTAMPDIFF(DAY, o.created_at, s.signed_at) BETWEEN 7 AND 15";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value.longValue();
    }

    private RowMapper<WarehouseInboundRecord> warehouseRowMapper() {
        return (rs, rowNum) -> {
            WarehouseInboundRecord record = new WarehouseInboundRecord();
            record.setInboundId(rs.getLong("inbound_id"));
            record.setOrderId(rs.getLong("order_id"));
            record.setTaskId(rs.getLong("task_id"));
            record.setBuyerId(rs.getLong("buyer_id"));
            record.setWarehouseCode(rs.getString("warehouse_code"));
            record.setWarehouseStatus(rs.getString("warehouse_status"));
            record.setQcStatus(rs.getString("qc_status"));
            record.setQcNote(rs.getString("qc_note"));
            record.setHandoverAt(toLocalDateTime(rs.getTimestamp("handover_at")));
            record.setScannedAt(toLocalDateTime(rs.getTimestamp("scanned_at")));
            record.setInspectedAt(toLocalDateTime(rs.getTimestamp("inspected_at")));
            record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return record;
        };
    }

    private RowMapper<CustomsClearanceRecord> customsRowMapper() {
        return (rs, rowNum) -> {
            CustomsClearanceRecord record = new CustomsClearanceRecord();
            record.setClearanceId(rs.getLong("clearance_id"));
            record.setOrderId(rs.getLong("order_id"));
            record.setDeclarationNo(rs.getString("declaration_no"));
            record.setClearanceStatus(rs.getString("clearance_status"));
            record.setComplianceChannel(rs.getString("compliance_channel"));
            record.setReviewComment(rs.getString("review_comment"));
            record.setSubmittedAt(toLocalDateTime(rs.getTimestamp("submitted_at")));
            record.setReviewedAt(toLocalDateTime(rs.getTimestamp("reviewed_at")));
            record.setReleasedAt(toLocalDateTime(rs.getTimestamp("released_at")));
            record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return record;
        };
    }

    private RowMapper<ShipmentTrackingRecord> shipmentRowMapper() {
        return (rs, rowNum) -> {
            ShipmentTrackingRecord record = new ShipmentTrackingRecord();
            record.setShipmentId(rs.getLong("shipment_id"));
            record.setOrderId(rs.getLong("order_id"));
            record.setCarrier(rs.getString("carrier"));
            record.setTrackingNo(rs.getString("tracking_no"));
            record.setShipmentStatus(rs.getString("shipment_status"));
            record.setLatestNode(rs.getString("latest_node"));
            record.setLatestNodeAt(toLocalDateTime(rs.getTimestamp("latest_node_at")));
            record.setSignedAt(toLocalDateTime(rs.getTimestamp("signed_at")));
            record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
            return record;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
