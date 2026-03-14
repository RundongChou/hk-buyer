package com.hkbuyer.repository;

import com.hkbuyer.domain.CatalogPublishStatus;
import com.hkbuyer.domain.CatalogSku;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbcTemplate;

    public CatalogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long createSpu(String spuName, String brandName, String categoryName) {
        String sql = "INSERT INTO sku_spu(spu_name, brand_name, category_name, audit_status, created_at, updated_at) " +
                "VALUES(?, ?, ?, 'APPROVED', NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, spuName);
            ps.setString(2, brandName);
            ps.setString(3, categoryName);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public boolean existsSpu(Long spuId) {
        String sql = "SELECT COUNT(1) FROM sku_spu WHERE spu_id = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, spuId);
        return count != null && count.longValue() > 0L;
    }

    public String getSpuBrandName(Long spuId) {
        String sql = "SELECT brand_name FROM sku_spu WHERE spu_id = ?";
        return jdbcTemplate.queryForObject(sql, String.class, spuId);
    }

    public Optional<String> findCategoryBySkuId(Long skuId) {
        String sql = "SELECT sp.category_name FROM sku_item s JOIN sku_spu sp ON sp.spu_id = s.spu_id WHERE s.sku_id = ?";
        List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("category_name"), skuId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(0));
    }

    public long createSku(Long spuId, String skuName, String specValue, String brandName) {
        String sql = "INSERT INTO sku_item(spu_id, sku_name, spec_value, brand_name, publish_status, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, NOW(), NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, spuId.longValue());
            ps.setString(2, skuName);
            ps.setString(3, specValue);
            ps.setString(4, brandName);
            ps.setString(5, CatalogPublishStatus.DRAFT.name());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void createPricePolicy(Long skuId, BigDecimal basePrice, BigDecimal serviceFeeRate, BigDecimal finalPrice) {
        String sql = "INSERT INTO sku_price_policy(sku_id, base_price, service_fee_rate, final_price, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, skuId, basePrice, serviceFeeRate, finalPrice);
    }

    public void createStockSnapshot(Long skuId, Integer availableQty, Integer alertThreshold, String reason) {
        String sql = "INSERT INTO stock_snapshot(sku_id, available_qty, locked_qty, alert_threshold, updated_reason, created_at, updated_at) " +
                "VALUES(?, ?, 0, ?, ?, NOW(), NOW())";
        jdbcTemplate.update(sql, skuId, availableQty, alertThreshold, reason);
    }

    public Optional<CatalogSku> findSkuById(Long skuId) {
        List<CatalogSku> items = queryCatalogSkus(" AND s.sku_id = ?", new Object[]{skuId}, 1);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(items.get(0));
    }

    public List<CatalogSku> listCatalogSkus(String keyword, boolean onlyPublished, int limit) {
        StringBuilder conditions = new StringBuilder();
        List<Object> args = new ArrayList<Object>();
        if (onlyPublished) {
            conditions.append(" AND s.publish_status = ?");
            args.add(CatalogPublishStatus.PUBLISHED.name());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String fuzzy = "%" + keyword.trim() + "%";
            conditions.append(" AND (s.sku_name LIKE ? OR sp.spu_name LIKE ? OR s.brand_name LIKE ?)");
            args.add(fuzzy);
            args.add(fuzzy);
            args.add(fuzzy);
        }
        return queryCatalogSkus(conditions.toString(), args.toArray(), limit);
    }

    public List<CatalogSku> listPendingAuditSkus(int limit) {
        return queryCatalogSkus(" AND s.publish_status = ?", new Object[]{CatalogPublishStatus.DRAFT.name()}, limit);
    }

    public void updatePublishStatus(Long skuId, CatalogPublishStatus status) {
        String sql = "UPDATE sku_item SET publish_status = ?, updated_at = NOW() WHERE sku_id = ?";
        jdbcTemplate.update(sql, status.name(), skuId);
    }

    public void addPublishAuditLog(Long skuId, Long adminId, String decision, String comment) {
        String sql = "INSERT INTO sku_publish_audit_log(sku_id, admin_id, decision, comment, created_at) VALUES(?, ?, ?, ?, NOW())";
        jdbcTemplate.update(sql, skuId, adminId, decision, comment);
    }

    public int adjustStock(Long skuId, Integer availableQty, Integer alertThreshold, String reason) {
        String sql = "UPDATE stock_snapshot SET available_qty = ?, alert_threshold = ?, updated_reason = ?, updated_at = NOW() WHERE sku_id = ?";
        return jdbcTemplate.update(sql, availableQty, alertThreshold, reason, skuId);
    }

    public int consumeStock(Long skuId, Integer qty, String reason) {
        String sql = "UPDATE stock_snapshot SET available_qty = available_qty - ?, updated_reason = ?, updated_at = NOW() " +
                "WHERE sku_id = ? AND available_qty >= ?";
        return jdbcTemplate.update(sql, qty, reason, skuId, qty);
    }

    public List<CatalogSku> listOutOfStockSkus(int limit) {
        String conditions = " AND s.publish_status = ? AND COALESCE(st.available_qty, 0) <= 0";
        return queryCatalogSkus(conditions, new Object[]{CatalogPublishStatus.PUBLISHED.name()}, limit);
    }

    public long countAllSkus() {
        String sql = "SELECT COUNT(1) FROM sku_item";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count.longValue();
    }

    public long countPublishedSkus() {
        String sql = "SELECT COUNT(1) FROM sku_item WHERE publish_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, CatalogPublishStatus.PUBLISHED.name());
        return count == null ? 0L : count.longValue();
    }

    public long countOutOfStockPublishedSkus() {
        String sql = "SELECT COUNT(1) FROM sku_item s JOIN stock_snapshot st ON st.sku_id = s.sku_id " +
                "WHERE s.publish_status = ? AND st.available_qty <= 0";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, CatalogPublishStatus.PUBLISHED.name());
        return count == null ? 0L : count.longValue();
    }

    private List<CatalogSku> queryCatalogSkus(String conditions, Object[] args, int limit) {
        String sql = "SELECT s.sku_id, s.spu_id, sp.spu_name, s.sku_name, s.spec_value, s.brand_name, s.publish_status, " +
                "pp.base_price, pp.service_fee_rate, pp.final_price, st.available_qty, st.locked_qty, st.alert_threshold, s.updated_at " +
                "FROM sku_item s " +
                "JOIN sku_spu sp ON sp.spu_id = s.spu_id " +
                "LEFT JOIN sku_price_policy pp ON pp.sku_id = s.sku_id " +
                "LEFT JOIN stock_snapshot st ON st.sku_id = s.sku_id " +
                "WHERE 1 = 1" + conditions + " " +
                "ORDER BY s.updated_at DESC, s.sku_id DESC LIMIT " + limit;
        return jdbcTemplate.query(sql, catalogSkuRowMapper(), args);
    }

    private RowMapper<CatalogSku> catalogSkuRowMapper() {
        return (rs, rowNum) -> {
            CatalogSku sku = new CatalogSku();
            sku.setSkuId(rs.getLong("sku_id"));
            sku.setSpuId(rs.getLong("spu_id"));
            sku.setSpuName(rs.getString("spu_name"));
            sku.setSkuName(rs.getString("sku_name"));
            sku.setSpecValue(rs.getString("spec_value"));
            sku.setBrandName(rs.getString("brand_name"));
            sku.setPublishStatus(CatalogPublishStatus.valueOf(rs.getString("publish_status")));
            sku.setBasePrice(rs.getBigDecimal("base_price"));
            sku.setServiceFeeRate(rs.getBigDecimal("service_fee_rate"));
            sku.setFinalPrice(rs.getBigDecimal("final_price"));

            int availableQty = rs.getInt("available_qty");
            if (!rs.wasNull()) {
                sku.setAvailableQty(Integer.valueOf(availableQty));
            }
            int lockedQty = rs.getInt("locked_qty");
            if (!rs.wasNull()) {
                sku.setLockedQty(Integer.valueOf(lockedQty));
            }
            int alertThreshold = rs.getInt("alert_threshold");
            if (!rs.wasNull()) {
                sku.setAlertThreshold(Integer.valueOf(alertThreshold));
            }
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            sku.setUpdatedAt(updatedAt == null ? null : updatedAt.toLocalDateTime());
            return sku;
        };
    }
}
