package com.hkbuyer.api;

import com.hkbuyer.service.CatalogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/skus")
    public List<Map<String, Object>> listSkus(@RequestParam(value = "keyword", required = false) String keyword,
                                              @RequestParam(value = "onlyPublished", defaultValue = "true") boolean onlyPublished) {
        return catalogService.listCatalogSkus(keyword, onlyPublished);
    }

    @GetMapping("/skus/{skuId}")
    public Map<String, Object> skuDetail(@PathVariable("skuId") Long skuId) {
        return catalogService.getSkuDetail(skuId);
    }
}
