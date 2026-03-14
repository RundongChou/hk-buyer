package com.hkbuyer.api;

import com.hkbuyer.api.dto.AuditProofRequest;
import com.hkbuyer.api.dto.AuditBuyerOnboardingRequest;
import com.hkbuyer.service.BuyerService;
import com.hkbuyer.service.MetricsService;
import com.hkbuyer.service.ProofService;
import com.hkbuyer.service.TaskService;
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
public class AdminController {

    private final ProofService proofService;
    private final MetricsService metricsService;
    private final BuyerService buyerService;
    private final TaskService taskService;

    public AdminController(ProofService proofService,
                           MetricsService metricsService,
                           BuyerService buyerService,
                           TaskService taskService) {
        this.proofService = proofService;
        this.metricsService = metricsService;
        this.buyerService = buyerService;
        this.taskService = taskService;
    }

    @GetMapping("/proofs/pending")
    public List<Map<String, Object>> listPendingProofs() {
        return proofService.listPendingProofs();
    }

    @PostMapping("/proofs/{proofId}/audit")
    public Map<String, Object> auditProof(@PathVariable("proofId") Long proofId,
                                          @Valid @RequestBody AuditProofRequest request) {
        return proofService.auditProof(proofId, request.getAdminId(), request.getDecision(), request.getComment());
    }

    @GetMapping("/metrics/funnel")
    public Map<String, Object> funnelMetrics() {
        return metricsService.buildFunnelMetrics();
    }

    @GetMapping("/buyer/onboarding/pending")
    public List<Map<String, Object>> pendingBuyerOnboarding() {
        return buyerService.listPendingOnboardingApplications();
    }

    @PostMapping("/buyer/onboarding/{applicationId}/audit")
    public Map<String, Object> auditBuyerOnboarding(@PathVariable("applicationId") Long applicationId,
                                                    @Valid @RequestBody AuditBuyerOnboardingRequest request) {
        return buyerService.auditOnboardingApplication(
                applicationId,
                request.getAdminId(),
                request.getDecision(),
                request.getComment()
        );
    }

    @GetMapping("/metrics/buyer-fulfillment")
    public Map<String, Object> buyerFulfillmentMetrics() {
        return metricsService.buildBuyerFulfillmentMetrics();
    }

    @GetMapping("/tasks/timeout-candidates")
    public List<Map<String, Object>> timeoutCandidates() {
        return taskService.listTimeoutCandidates();
    }

    @PostMapping("/tasks/timeout-reprice/run")
    public Map<String, Object> runTimeoutReprice() {
        return taskService.runTimeoutReprice();
    }

    @GetMapping("/metrics/dynamic-pricing")
    public Map<String, Object> dynamicPricingMetrics() {
        return metricsService.buildDynamicPricingMetrics();
    }
}
