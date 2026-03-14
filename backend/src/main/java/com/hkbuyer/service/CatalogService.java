package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.BatchImportSkuItemRequest;
import com.hkbuyer.api.dto.BatchImportSkusRequest;
import com.hkbuyer.api.dto.CreateSpuRequest;
import com.hkbuyer.domain.CatalogPublishStatus;
import com.hkbuyer.domain.CatalogSku;
import com.hkbuyer.repository.CatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final CatalogRepository catalogRepository;

    public CatalogService(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Transactional
    public Map<String, Object> createSpu(CreateSpuRequest request) {
        long spuId = catalogRepository.createSpu(
                request.getSpuName().trim(),
                request.getBrandName().trim(),
                request.getCategoryName().trim()
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("spuId", spuId);
        payload.put("spuName", request.getSpuName().trim());
        payload.put("brandName", request.getBrandName().trim());
        payload.put("categoryName", request.getCategoryName().trim());
        payload.put("auditStatus", "APPROVED");
        return payload;
    }

    @Transactional
    public Map<String, Object> batchImportSkus(BatchImportSkusRequest request) {
        if (!catalogRepository.existsSpu(request.getSpuId())) {
            throw new ApiException("spu not found: " + request.getSpuId());
        }

        String brandName = catalogRepository.getSpuBrandName(request.getSpuId());
        List<Long> skuIds = new ArrayList<Long>();

        for (BatchImportSkuItemRequest item : request.getItems()) {
            if (item.getServiceFeeRate().compareTo(new BigDecimal("1.0000")) > 0) {
                throw new ApiException("serviceFeeRate must be <= 1.0000");
            }
            int alertThreshold = item.getAlertThreshold() == null ? 5 : item.getAlertThreshold().intValue();
            BigDecimal finalPrice = item.getBasePrice()
                    .multiply(BigDecimal.ONE.add(item.getServiceFeeRate()))
                    .setScale(2, RoundingMode.HALF_UP);

            long skuId = catalogRepository.createSku(
                    request.getSpuId(),
                    item.getSkuName().trim(),
                    item.getSpecValue().trim(),
                    brandName
            );
            catalogRepository.createPricePolicy(skuId, item.getBasePrice(), item.getServiceFeeRate(), finalPrice);
            catalogRepository.createStockSnapshot(skuId, item.getAvailableQty(), Integer.valueOf(alertThreshold), "batch_import");
            skuIds.add(Long.valueOf(skuId));
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("spuId", request.getSpuId());
        payload.put("importedCount", Integer.valueOf(skuIds.size()));
        payload.put("skuIds", skuIds);
        payload.put("publishStatus", CatalogPublishStatus.DRAFT.name());
        return payload;
    }

    public List<Map<String, Object>> listCatalogSkus(String keyword, boolean onlyPublished) {
        return catalogRepository.listCatalogSkus(keyword, onlyPublished, 200).stream()
                .map(this::toSkuPayload)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getSkuDetail(Long skuId) {
        CatalogSku sku = catalogRepository.findSkuById(skuId)
                .orElseThrow(() -> new ApiException("sku not found: " + skuId));
        return toSkuPayload(sku);
    }

    public List<Map<String, Object>> listPendingAuditSkus() {
        return catalogRepository.listPendingAuditSkus(200).stream()
                .map(this::toSkuPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> auditPublish(Long skuId, Long adminId, String decision, String comment) {
        CatalogSku sku = catalogRepository.findSkuById(skuId)
                .orElseThrow(() -> new ApiException("sku not found: " + skuId));

        String normalized = decision.trim().toUpperCase(Locale.ROOT);
        CatalogPublishStatus status;
        if ("APPROVE".equals(normalized) || "APPROVED".equals(normalized)) {
            status = CatalogPublishStatus.PUBLISHED;
        } else if ("REJECT".equals(normalized) || "REJECTED".equals(normalized)) {
            status = CatalogPublishStatus.REJECTED;
        } else {
            throw new ApiException("decision must be APPROVE or REJECT");
        }

        catalogRepository.updatePublishStatus(skuId, status);
        catalogRepository.addPublishAuditLog(skuId, adminId, status.name(), comment);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("skuId", skuId);
        payload.put("previousStatus", sku.getPublishStatus().name());
        payload.put("publishStatus", status.name());
        payload.put("adminId", adminId);
        return payload;
    }

    @Transactional
    public Map<String, Object> adjustStock(Long skuId, Integer availableQty, Integer alertThreshold, String reason) {
        CatalogSku sku = catalogRepository.findSkuById(skuId)
                .orElseThrow(() -> new ApiException("sku not found: " + skuId));
        int normalizedAlertThreshold = alertThreshold == null ? 5 : alertThreshold.intValue();
        String normalizedReason = reason == null || reason.trim().isEmpty() ? "manual_adjust" : reason.trim();

        int affected = catalogRepository.adjustStock(skuId, availableQty, Integer.valueOf(normalizedAlertThreshold), normalizedReason);
        if (affected == 0) {
            throw new ApiException("stock snapshot not found for sku: " + skuId);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("skuId", skuId);
        payload.put("skuName", sku.getSkuName());
        payload.put("availableQty", availableQty);
        payload.put("alertThreshold", normalizedAlertThreshold);
        payload.put("reason", normalizedReason);
        return payload;
    }

    public List<Map<String, Object>> listOutOfStockAlerts() {
        return catalogRepository.listOutOfStockSkus(200).stream().map(sku -> {
            Map<String, Object> payload = toSkuPayload(sku);
            payload.put("alertType", "OUT_OF_STOCK");
            return payload;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> buildCatalogMetrics() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("sku_total", catalogRepository.countAllSkus());
        payload.put("sku_published", catalogRepository.countPublishedSkus());
        payload.put("sku_out_of_stock", catalogRepository.countOutOfStockPublishedSkus());
        return payload;
    }

    public ResolvedOrderLine resolveOrderLine(Long skuId, Integer qty, BigDecimal fallbackUnitPrice) {
        Optional<CatalogSku> optionalSku = catalogRepository.findSkuById(skuId);
        if (!optionalSku.isPresent()) {
            if (fallbackUnitPrice == null || fallbackUnitPrice.signum() <= 0) {
                throw new ApiException("sku not found in catalog and unitPrice is required: " + skuId);
            }
            return new ResolvedOrderLine(skuId, qty, fallbackUnitPrice, false);
        }

        CatalogSku sku = optionalSku.get();
        if (sku.getPublishStatus() != CatalogPublishStatus.PUBLISHED) {
            throw new ApiException("sku is not published: " + skuId);
        }

        int availableQty = sku.getAvailableQty() == null ? 0 : sku.getAvailableQty().intValue();
        if (availableQty < qty.intValue()) {
            throw new ApiException("sku out of stock: " + skuId);
        }

        BigDecimal orderPrice = sku.getFinalPrice();
        if (orderPrice == null) {
            if (fallbackUnitPrice == null || fallbackUnitPrice.signum() <= 0) {
                throw new ApiException("price policy missing for sku: " + skuId);
            }
            orderPrice = fallbackUnitPrice;
        }

        return new ResolvedOrderLine(skuId, qty, orderPrice, true);
    }

    public void consumeStockForOrder(ResolvedOrderLine orderLine, Long orderId) {
        if (!orderLine.isCatalogManaged()) {
            return;
        }
        int affected = catalogRepository.consumeStock(
                orderLine.getSkuId(),
                orderLine.getQty(),
                "order_" + orderId + "_created"
        );
        if (affected == 0) {
            throw new ApiException("stock not enough for sku: " + orderLine.getSkuId());
        }
    }

    private Map<String, Object> toSkuPayload(CatalogSku sku) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        boolean saleable = sku.getPublishStatus() == CatalogPublishStatus.PUBLISHED
                && sku.getAvailableQty() != null
                && sku.getAvailableQty().intValue() > 0;
        String stockStatus;
        if (sku.getAvailableQty() == null || sku.getAvailableQty().intValue() <= 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (sku.getAlertThreshold() != null && sku.getAvailableQty().intValue() <= sku.getAlertThreshold().intValue()) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }

        payload.put("skuId", sku.getSkuId());
        payload.put("spuId", sku.getSpuId());
        payload.put("spuName", sku.getSpuName());
        payload.put("skuName", sku.getSkuName());
        payload.put("specValue", sku.getSpecValue());
        payload.put("brandName", sku.getBrandName());
        payload.put("publishStatus", sku.getPublishStatus().name());
        payload.put("basePrice", sku.getBasePrice());
        payload.put("serviceFeeRate", sku.getServiceFeeRate());
        payload.put("finalPrice", sku.getFinalPrice());
        payload.put("availableQty", sku.getAvailableQty());
        payload.put("lockedQty", sku.getLockedQty());
        payload.put("alertThreshold", sku.getAlertThreshold());
        payload.put("stockStatus", stockStatus);
        payload.put("saleable", Boolean.valueOf(saleable));
        payload.put("updatedAt", sku.getUpdatedAt());
        return payload;
    }

    public static class ResolvedOrderLine {
        private final Long skuId;
        private final Integer qty;
        private final BigDecimal unitPrice;
        private final boolean catalogManaged;

        public ResolvedOrderLine(Long skuId, Integer qty, BigDecimal unitPrice, boolean catalogManaged) {
            this.skuId = skuId;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.catalogManaged = catalogManaged;
        }

        public Long getSkuId() {
            return skuId;
        }

        public Integer getQty() {
            return qty;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public boolean isCatalogManaged() {
            return catalogManaged;
        }
    }
}
