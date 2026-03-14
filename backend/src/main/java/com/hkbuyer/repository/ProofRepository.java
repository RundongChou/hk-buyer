package com.hkbuyer.repository;

import com.hkbuyer.domain.ProofAuditStatus;
import com.hkbuyer.domain.PurchaseProof;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ProofRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProofRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createProof(Long taskId,
                            Long buyerId,
                            String storeName,
                            String receiptUrl,
                            String batchNo,
                            LocalDate expiryDate,
                            String productPhotoUrl) {
        String sql = "INSERT INTO purchase_proof(task_id, buyer_id, store_name, receipt_url, batch_no, expiry_date, product_photo_url, " +
                "audit_status, audited_by, audit_comment, created_at, audited_at) VALUES(?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NOW(), NULL)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, taskId.longValue());
            ps.setLong(2, buyerId.longValue());
            ps.setString(3, storeName);
            ps.setString(4, receiptUrl);
            ps.setString(5, batchNo);
            ps.setDate(6, Date.valueOf(expiryDate));
            ps.setString(7, productPhotoUrl);
            ps.setString(8, ProofAuditStatus.PENDING.name());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<PurchaseProof> findById(Long proofId) {
        String sql = "SELECT proof_id, task_id, buyer_id, store_name, receipt_url, batch_no, expiry_date, product_photo_url, " +
                "audit_status, audited_by, audit_comment, created_at, audited_at FROM purchase_proof WHERE proof_id = ?";
        List<PurchaseProof> proofs = jdbcTemplate.query(sql, proofRowMapper(), proofId);
        if (proofs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(proofs.get(0));
    }

    public List<PurchaseProof> listPendingProofs() {
        String sql = "SELECT proof_id, task_id, buyer_id, store_name, receipt_url, batch_no, expiry_date, product_photo_url, " +
                "audit_status, audited_by, audit_comment, created_at, audited_at FROM purchase_proof WHERE audit_status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, proofRowMapper(), ProofAuditStatus.PENDING.name());
    }

    public void auditProof(Long proofId, ProofAuditStatus auditStatus, Long adminId, String comment) {
        String sql = "UPDATE purchase_proof SET audit_status = ?, audited_by = ?, audit_comment = ?, audited_at = NOW() WHERE proof_id = ?";
        jdbcTemplate.update(sql, auditStatus.name(), adminId, comment, proofId);
    }

    public long countSubmittedProofs() {
        String sql = "SELECT COUNT(1) FROM purchase_proof";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result == null ? 0L : result.longValue();
    }

    private RowMapper<PurchaseProof> proofRowMapper() {
        return (rs, rowNum) -> {
            PurchaseProof proof = new PurchaseProof();
            proof.setProofId(rs.getLong("proof_id"));
            proof.setTaskId(rs.getLong("task_id"));
            proof.setBuyerId(rs.getLong("buyer_id"));
            proof.setStoreName(rs.getString("store_name"));
            proof.setReceiptUrl(rs.getString("receipt_url"));
            proof.setBatchNo(rs.getString("batch_no"));
            Date expiryDate = rs.getDate("expiry_date");
            proof.setExpiryDate(expiryDate == null ? null : expiryDate.toLocalDate());
            proof.setProductPhotoUrl(rs.getString("product_photo_url"));
            proof.setAuditStatus(ProofAuditStatus.valueOf(rs.getString("audit_status")));
            Long auditedBy = rs.getLong("audited_by");
            if (!rs.wasNull()) {
                proof.setAuditedBy(auditedBy);
            }
            proof.setAuditComment(rs.getString("audit_comment"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp auditedAt = rs.getTimestamp("audited_at");
            proof.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            proof.setAuditedAt(auditedAt == null ? null : auditedAt.toLocalDateTime());
            return proof;
        };
    }
}
