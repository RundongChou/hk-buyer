package com.hkbuyer.api;

import com.hkbuyer.api.dto.SubmitBuyerOnboardingRequest;
import com.hkbuyer.service.BuyerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/buyer")
public class BuyerController {

    private final BuyerService buyerService;

    public BuyerController(BuyerService buyerService) {
        this.buyerService = buyerService;
    }

    @PostMapping("/onboarding/applications")
    public Map<String, Object> submitOnboardingApplication(@Valid @RequestBody SubmitBuyerOnboardingRequest request) {
        return buyerService.submitOnboardingApplication(request);
    }

    @GetMapping("/profile/{buyerId}")
    public Map<String, Object> buyerProfile(@PathVariable("buyerId") Long buyerId) {
        return buyerService.getBuyerProfile(buyerId);
    }
}
