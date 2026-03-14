package com.hkbuyer.api;

import com.hkbuyer.api.dto.AuditProofRequest;
import com.hkbuyer.api.dto.ArbitrateAfterSaleCaseRequest;
import com.hkbuyer.api.dto.AuditBuyerOnboardingRequest;
import com.hkbuyer.api.dto.InboundScanRequest;
import com.hkbuyer.api.dto.ReviewCustomsRequest;
import com.hkbuyer.api.dto.SubmitCustomsRequest;
import com.hkbuyer.api.dto.UpdateShipmentRequest;
import com.hkbuyer.service.AfterSaleService;
import com.hkbuyer.service.BuyerService;
import com.hkbuyer.service.FulfillmentService;
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
    private final FulfillmentService fulfillmentService;
    private final AfterSaleService afterSaleService;

    public AdminController(ProofService proofService,
                           MetricsService metricsService,
                           BuyerService buyerService,
                           TaskService taskService,
                           FulfillmentService fulfillmentService,
                           AfterSaleService afterSaleService) {
        this.proofService = proofService;
        this.metricsService = metricsService;
        this.buyerService = buyerService;
        this.taskService = taskService;
        this.fulfillmentService = fulfillmentService;
        this.afterSaleService = afterSaleService;
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

    @PostMapping("/fulfillment/inbound/scan")
    public Map<String, Object> scanInbound(@Valid @RequestBody InboundScanRequest request) {
        return fulfillmentService.scanInbound(
                request.getTaskId(),
                request.getWarehouseCode(),
                request.getQcDecision(),
                request.getQcNote()
        );
    }

    @PostMapping("/fulfillment/customs/submit")
    public Map<String, Object> submitCustoms(@Valid @RequestBody SubmitCustomsRequest request) {
        return fulfillmentService.submitCustoms(
                request.getOrderId(),
                request.getDeclarationNo(),
                request.getComplianceChannel()
        );
    }

    @PostMapping("/fulfillment/customs/review")
    public Map<String, Object> reviewCustoms(@Valid @RequestBody ReviewCustomsRequest request) {
        return fulfillmentService.reviewCustoms(
                request.getOrderId(),
                request.getDecision(),
                request.getComment()
        );
    }

    @PostMapping("/fulfillment/shipment/update")
    public Map<String, Object> updateShipment(@Valid @RequestBody UpdateShipmentRequest request) {
        return fulfillmentService.updateShipment(
                request.getOrderId(),
                request.getCarrier(),
                request.getTrackingNo(),
                request.getShipmentStatus(),
                request.getLatestNode()
        );
    }

    @GetMapping("/fulfillment/orders/{orderId}")
    public Map<String, Object> fulfillmentDetail(@PathVariable("orderId") Long orderId) {
        return fulfillmentService.getOrderFulfillment(orderId);
    }

    @GetMapping("/metrics/fulfillment")
    public Map<String, Object> fulfillmentMetrics() {
        return metricsService.buildFulfillmentMetrics();
    }

    @GetMapping("/after-sale/cases/pending")
    public List<Map<String, Object>> pendingAfterSaleCases() {
        return afterSaleService.listPendingArbitrationCases();
    }

    @PostMapping("/after-sale/cases/{caseId}/arbitrate")
    public Map<String, Object> arbitrateAfterSaleCase(@PathVariable("caseId") Long caseId,
                                                      @Valid @RequestBody ArbitrateAfterSaleCaseRequest request) {
        return afterSaleService.arbitrateCase(
                caseId,
                request.getAdminId(),
                request.getDecision(),
                request.getComment(),
                request.getFinalRefundAmount()
        );
    }

    @GetMapping("/metrics/after-sale-risk")
    public Map<String, Object> afterSaleRiskMetrics() {
        return metricsService.buildAfterSaleRiskMetrics();
    }
}
