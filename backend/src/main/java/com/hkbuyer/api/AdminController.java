package com.hkbuyer.api;

import com.hkbuyer.api.dto.AuditProofRequest;
import com.hkbuyer.api.dto.ArbitrateAfterSaleCaseRequest;
import com.hkbuyer.api.dto.ActivateOpsExperimentRequest;
import com.hkbuyer.api.dto.AuditBuyerOnboardingRequest;
import com.hkbuyer.api.dto.CompleteSettlementPayoutRequest;
import com.hkbuyer.api.dto.CreateGrowthCampaignRequest;
import com.hkbuyer.api.dto.CreateOpsExperimentRequest;
import com.hkbuyer.api.dto.InboundScanRequest;
import com.hkbuyer.api.dto.PublishGrowthCampaignRequest;
import com.hkbuyer.api.dto.ReconcileSettlementLedgerRequest;
import com.hkbuyer.api.dto.ReviewCustomsRequest;
import com.hkbuyer.api.dto.SubmitCustomsRequest;
import com.hkbuyer.api.dto.UpdateShipmentRequest;
import com.hkbuyer.service.AfterSaleService;
import com.hkbuyer.service.BuyerService;
import com.hkbuyer.service.FulfillmentService;
import com.hkbuyer.service.GrowthService;
import com.hkbuyer.service.MetricsService;
import com.hkbuyer.service.OptimizationService;
import com.hkbuyer.service.ProofService;
import com.hkbuyer.service.SettlementService;
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
    private final SettlementService settlementService;
    private final GrowthService growthService;
    private final OptimizationService optimizationService;

    public AdminController(ProofService proofService,
                           MetricsService metricsService,
                           BuyerService buyerService,
                           TaskService taskService,
                           FulfillmentService fulfillmentService,
                           AfterSaleService afterSaleService,
                           SettlementService settlementService,
                           GrowthService growthService,
                           OptimizationService optimizationService) {
        this.proofService = proofService;
        this.metricsService = metricsService;
        this.buyerService = buyerService;
        this.taskService = taskService;
        this.fulfillmentService = fulfillmentService;
        this.afterSaleService = afterSaleService;
        this.settlementService = settlementService;
        this.growthService = growthService;
        this.optimizationService = optimizationService;
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

    @GetMapping("/settlements/pending-payout")
    public List<Map<String, Object>> pendingPayoutSettlements() {
        return settlementService.listPendingPayoutLedgers();
    }

    @PostMapping("/settlements/{ledgerId}/complete-payout")
    public Map<String, Object> completeSettlementPayout(@PathVariable("ledgerId") Long ledgerId,
                                                        @Valid @RequestBody CompleteSettlementPayoutRequest request) {
        return settlementService.completePayout(ledgerId, request.getAdminId(), request.getComment());
    }

    @PostMapping("/settlements/{ledgerId}/reconcile")
    public Map<String, Object> reconcileSettlement(@PathVariable("ledgerId") Long ledgerId,
                                                   @Valid @RequestBody ReconcileSettlementLedgerRequest request) {
        return settlementService.reconcileLedger(
                ledgerId,
                request.getAdminId(),
                request.getDecision(),
                request.getExceptionReason()
        );
    }

    @GetMapping("/settlements/reconciliation/report")
    public Map<String, Object> settlementReconciliationReport() {
        return settlementService.buildReconciliationReport();
    }

    @GetMapping("/metrics/settlement")
    public Map<String, Object> settlementMetrics() {
        return metricsService.buildSettlementMetrics();
    }

    @PostMapping("/growth/campaigns")
    public Map<String, Object> createGrowthCampaign(@Valid @RequestBody CreateGrowthCampaignRequest request) {
        return growthService.createCampaign(request);
    }

    @GetMapping("/growth/campaigns")
    public List<Map<String, Object>> listGrowthCampaigns() {
        return growthService.listCampaigns();
    }

    @PostMapping("/growth/campaigns/{campaignId}/publish")
    public Map<String, Object> publishGrowthCampaign(@PathVariable("campaignId") Long campaignId,
                                                     @Valid @RequestBody PublishGrowthCampaignRequest request) {
        return growthService.publishCampaign(campaignId, request.getAdminId(), request.getUserIds(), request.getTouchChannel());
    }

    @GetMapping("/metrics/growth")
    public Map<String, Object> growthMetrics() {
        return metricsService.buildGrowthMetrics();
    }

    @PostMapping("/ops/experiments")
    public Map<String, Object> createOpsExperiment(@Valid @RequestBody CreateOpsExperimentRequest request) {
        return optimizationService.createExperiment(request);
    }

    @PostMapping("/ops/experiments/{experimentId}/activate")
    public Map<String, Object> activateOpsExperiment(@PathVariable("experimentId") Long experimentId,
                                                     @Valid @RequestBody ActivateOpsExperimentRequest request) {
        return optimizationService.activateExperiment(experimentId, request.getAdminId());
    }

    @GetMapping("/ops/experiments/active")
    public Map<String, Object> activeOpsExperiment() {
        return optimizationService.getActiveExperiment();
    }

    @GetMapping("/ops/experiments/{experimentId}/assignments")
    public List<Map<String, Object>> listOpsExperimentAssignments(@PathVariable("experimentId") Long experimentId) {
        return optimizationService.listAssignments(experimentId, 200);
    }

    @PostMapping("/ops/alerts/evaluate")
    public Map<String, Object> evaluateOpsAlerts() {
        return optimizationService.evaluateAlerts();
    }

    @GetMapping("/ops/alerts/open")
    public List<Map<String, Object>> openOpsAlerts() {
        return optimizationService.listOpenAlerts(200);
    }

    @GetMapping("/metrics/ops-optimization")
    public Map<String, Object> opsOptimizationMetrics() {
        return metricsService.buildOpsOptimizationMetrics();
    }
}
