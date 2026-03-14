package com.hkbuyer.api;

import com.hkbuyer.api.dto.BatchImportSkusRequest;
import com.hkbuyer.api.dto.CreateSpuRequest;
import com.hkbuyer.api.dto.PublishAuditRequest;
import com.hkbuyer.api.dto.StockAdjustRequest;
import com.hkbuyer.service.CatalogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class CatalogAdminController {

    private final CatalogService catalogService;

    public CatalogAdminController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/catalog/spus")
    public Map<String, Object> createSpu(@Valid @RequestBody CreateSpuRequest request) {
        return catalogService.createSpu(request);
    }

    @PostMapping("/catalog/skus/batch-import")
    public Map<String, Object> batchImportSkus(@Valid @RequestBody BatchImportSkusRequest request) {
        return catalogService.batchImportSkus(request);
    }

    @GetMapping("/catalog/skus/pending-audit")
    public List<Map<String, Object>> pendingAuditSkus() {
        return catalogService.listPendingAuditSkus();
    }

    @PostMapping("/catalog/skus/{skuId}/publish-audit")
    public Map<String, Object> auditPublish(@PathVariable("skuId") Long skuId,
                                            @Valid @RequestBody PublishAuditRequest request) {
        return catalogService.auditPublish(skuId, request.getAdminId(), request.getDecision(), request.getComment());
    }

    @PostMapping("/catalog/skus/{skuId}/stock-adjust")
    public Map<String, Object> adjustStock(@PathVariable("skuId") Long skuId,
                                           @Valid @RequestBody StockAdjustRequest request) {
        return catalogService.adjustStock(skuId, request.getAvailableQty(), request.getAlertThreshold(), request.getReason());
    }

    @GetMapping("/inventory/alerts/out-of-stock")
    public List<Map<String, Object>> outOfStockAlerts() {
        return catalogService.listOutOfStockAlerts();
    }

    @GetMapping("/metrics/catalog")
    public Map<String, Object> catalogMetrics() {
        return catalogService.buildCatalogMetrics();
    }
}
