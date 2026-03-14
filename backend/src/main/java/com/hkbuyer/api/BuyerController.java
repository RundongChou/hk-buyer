package com.hkbuyer.api;

import com.hkbuyer.api.dto.RequestPayoutRequest;
import com.hkbuyer.api.dto.SubmitBuyerOnboardingRequest;
import com.hkbuyer.service.BuyerService;
import com.hkbuyer.service.SettlementService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/buyer")
public class BuyerController {

    private final BuyerService buyerService;
    private final SettlementService settlementService;

    public BuyerController(BuyerService buyerService, SettlementService settlementService) {
        this.buyerService = buyerService;
        this.settlementService = settlementService;
    }

    @PostMapping("/onboarding/applications")
    public Map<String, Object> submitOnboardingApplication(@Valid @RequestBody SubmitBuyerOnboardingRequest request) {
        return buyerService.submitOnboardingApplication(request);
    }

    @GetMapping("/profile/{buyerId}")
    public Map<String, Object> buyerProfile(@PathVariable("buyerId") Long buyerId) {
        return buyerService.getBuyerProfile(buyerId);
    }

    @GetMapping("/settlements")
    public List<Map<String, Object>> listSettlements(@RequestParam("buyerId") Long buyerId,
                                                     @RequestParam(value = "settlementStatus", required = false) String settlementStatus) {
        return settlementService.listBuyerLedgers(buyerId, settlementStatus);
    }

    @PostMapping("/settlements/{ledgerId}/request-payout")
    public Map<String, Object> requestPayout(@PathVariable("ledgerId") Long ledgerId,
                                             @Valid @RequestBody RequestPayoutRequest request) {
        return settlementService.requestPayout(ledgerId, request.getBuyerId());
    }
}
