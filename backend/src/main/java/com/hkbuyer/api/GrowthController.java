package com.hkbuyer.api;

import com.hkbuyer.service.GrowthService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/growth")
public class GrowthController {

    private final GrowthService growthService;

    public GrowthController(GrowthService growthService) {
        this.growthService = growthService;
    }

    @GetMapping("/membership")
    public Map<String, Object> membership(@RequestParam("userId") Long userId) {
        return growthService.getMembershipProfile(userId);
    }

    @GetMapping("/coupons")
    public Map<String, Object> coupons(@RequestParam("userId") Long userId) {
        return growthService.listUserCoupons(userId);
    }

    @GetMapping("/recommendations")
    public Map<String, Object> recommendations(@RequestParam("userId") Long userId) {
        return growthService.getRecommendations(userId);
    }
}
