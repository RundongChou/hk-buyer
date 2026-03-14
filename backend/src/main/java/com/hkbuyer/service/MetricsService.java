package com.hkbuyer.service;

import com.hkbuyer.domain.BuyerLevel;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MetricsService {

    private final OrderService orderService;
    private final TaskService taskService;
    private final ProofService proofService;
    private final BuyerService buyerService;
    private final FulfillmentService fulfillmentService;
    private final AfterSaleService afterSaleService;
    private final SettlementService settlementService;
    private final GrowthService growthService;

    public MetricsService(OrderService orderService,
                          TaskService taskService,
                          ProofService proofService,
                          BuyerService buyerService,
                          FulfillmentService fulfillmentService,
                          AfterSaleService afterSaleService,
                          SettlementService settlementService,
                          GrowthService growthService) {
        this.orderService = orderService;
        this.taskService = taskService;
        this.proofService = proofService;
        this.buyerService = buyerService;
        this.fulfillmentService = fulfillmentService;
        this.afterSaleService = afterSaleService;
        this.settlementService = settlementService;
        this.growthService = growthService;
    }

    public Map<String, Object> buildFunnelMetrics() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("payment_success", orderService.countPaidOrders());
        payload.put("payment_failed", orderService.countPaymentFailedEvents());
        payload.put("payment_compensated", orderService.countPaymentCompensatedEvents());
        payload.put("task_accepted", taskService.countAcceptedTasks());
        payload.put("proof_submitted", proofService.countSubmittedProofs());
        return payload;
    }

    public Map<String, Object> buildBuyerFulfillmentMetrics() {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("buyer_pending_applications", buyerService.countPendingApplications());
        payload.put("buyer_approved_total", buyerService.countApprovedProfiles());
        payload.put("task_timeout_unaccepted_72h", taskService.countTimeoutUnacceptedTasks());
        payload.put("buyer_bronze_total", buyerService.countProfilesByLevel(BuyerLevel.BRONZE));
        payload.put("buyer_silver_total", buyerService.countProfilesByLevel(BuyerLevel.SILVER));
        payload.put("buyer_gold_total", buyerService.countProfilesByLevel(BuyerLevel.GOLD));
        return payload;
    }

    public Map<String, Object> buildDynamicPricingMetrics() {
        long timeoutCandidates = taskService.countTimeoutCandidates();
        long frequencyLimited = taskService.countFrequencyLimitedTimeoutCandidates();
        long autoMarkupTaskTotal = taskService.countTasksWithAutoMarkup();
        long autoMarkupAppliedTotal = taskService.sumAutoMarkupAppliedCount();
        long redispatchTotal = taskService.sumRedispatchCount();
        long terminatedTotal = taskService.countTimeoutTerminatedTasks();
        long acceptedAfterMarkup = taskService.countAcceptedAfterAutoMarkup();

        double acceptedRate = 0D;
        if (autoMarkupTaskTotal > 0L) {
            acceptedRate = (acceptedAfterMarkup * 1.0D) / autoMarkupTaskTotal;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("timeout_candidates_total", Long.valueOf(timeoutCandidates));
        payload.put("timeout_candidates_frequency_limited", Long.valueOf(frequencyLimited));
        payload.put("auto_markup_task_total", Long.valueOf(autoMarkupTaskTotal));
        payload.put("auto_markup_applied_total", Long.valueOf(autoMarkupAppliedTotal));
        payload.put("task_redispatch_total", Long.valueOf(redispatchTotal));
        payload.put("task_timeout_terminated_total", Long.valueOf(terminatedTotal));
        payload.put("repriced_task_accepted_total", Long.valueOf(acceptedAfterMarkup));
        payload.put("repriced_task_accept_rate", Double.valueOf(acceptedRate));
        return payload;
    }

    public Map<String, Object> buildFulfillmentMetrics() {
        long inboundCompleted = fulfillmentService.countInboundCompleted();
        long customsSubmitted = fulfillmentService.countCustomsSubmitted();
        long customsReleased = fulfillmentService.countCustomsReleased();
        long customsRejected = fulfillmentService.countCustomsRejected();
        long shipmentInTransit = fulfillmentService.countShipmentInTransit();
        long shipmentSigned = fulfillmentService.countShipmentSigned();
        long signedWithinSevenToFifteenDays = fulfillmentService.countSignedWithinSevenToFifteenDays();

        double customsSuccessRate = 0D;
        if (customsSubmitted > 0L) {
            customsSuccessRate = (customsReleased * 1.0D) / customsSubmitted;
        }

        double signedWithinTargetRate = 0D;
        if (shipmentSigned > 0L) {
            signedWithinTargetRate = (signedWithinSevenToFifteenDays * 1.0D) / shipmentSigned;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("warehouse_inbound_completed_total", Long.valueOf(inboundCompleted));
        payload.put("customs_submitted_total", Long.valueOf(customsSubmitted));
        payload.put("customs_released_total", Long.valueOf(customsReleased));
        payload.put("customs_rejected_total", Long.valueOf(customsRejected));
        payload.put("customs_success_rate", Double.valueOf(customsSuccessRate));
        payload.put("compliance_clearance_success_rate", Double.valueOf(customsSuccessRate));
        payload.put("shipment_in_transit_total", Long.valueOf(shipmentInTransit));
        payload.put("shipment_signed_total", Long.valueOf(shipmentSigned));
        payload.put("signed_within_7_15_days_total", Long.valueOf(signedWithinSevenToFifteenDays));
        payload.put("signed_within_7_15_days_rate", Double.valueOf(signedWithinTargetRate));
        return payload;
    }

    public Map<String, Object> buildAfterSaleRiskMetrics() {
        long openCases = afterSaleService.countOpenCases();
        long pendingArbitration = afterSaleService.countPendingArbitrationCases();
        long resolvedCases = afterSaleService.countResolvedCases();
        long counterfeitDisputes = afterSaleService.countCounterfeitDisputes();
        long partialRefundApproved = afterSaleService.countApprovedPartialRefundCases();
        long cancelledOrders = orderService.countCancelledOrders();
        long totalOrders = orderService.countTotalOrders();
        long paidOrders = orderService.countPaidOrders();

        double counterfeitComplaintRate = 0D;
        if (paidOrders > 0L) {
            counterfeitComplaintRate = (counterfeitDisputes * 1.0D) / paidOrders;
        }

        double orderCancelRate = 0D;
        if (totalOrders > 0L) {
            orderCancelRate = (cancelledOrders * 1.0D) / totalOrders;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("after_sale_open_cases_total", Long.valueOf(openCases));
        payload.put("after_sale_pending_arbitration_total", Long.valueOf(pendingArbitration));
        payload.put("after_sale_resolved_total", Long.valueOf(resolvedCases));
        payload.put("counterfeit_dispute_total", Long.valueOf(counterfeitDisputes));
        payload.put("counterfeit_complaint_rate", Double.valueOf(counterfeitComplaintRate));
        payload.put("partial_refund_approved_total", Long.valueOf(partialRefundApproved));
        payload.put("order_cancelled_total", Long.valueOf(cancelledOrders));
        payload.put("order_cancel_rate", Double.valueOf(orderCancelRate));
        return payload;
    }

    public Map<String, Object> buildSettlementMetrics() {
        long settlementLedgerTotal = settlementService.countTotalLedgers();
        long settlementPendingTotal = settlementService.countPendingLedgers();
        long settlementPayoutRequestedTotal = settlementService.countPayoutRequestedLedgers();
        long settlementSettledTotal = settlementService.countSettledLedgers();
        long settlementReconciliationMatchedTotal = settlementService.countReconciliationMatchedLedgers();
        long settlementReconciliationExceptionTotal = settlementService.countReconciliationExceptionLedgers();

        double settlementCompletionRate = 0D;
        if (settlementLedgerTotal > 0L) {
            settlementCompletionRate = (settlementSettledTotal * 1.0D) / settlementLedgerTotal;
        }

        long reconciliationProcessedTotal = settlementReconciliationMatchedTotal + settlementReconciliationExceptionTotal;
        double settlementReconciliationAccuracyRate = 0D;
        if (reconciliationProcessedTotal > 0L) {
            settlementReconciliationAccuracyRate = (settlementReconciliationMatchedTotal * 1.0D) / reconciliationProcessedTotal;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("settlement_ledger_total", Long.valueOf(settlementLedgerTotal));
        payload.put("settlement_pending_total", Long.valueOf(settlementPendingTotal));
        payload.put("settlement_payout_requested_total", Long.valueOf(settlementPayoutRequestedTotal));
        payload.put("settlement_settled_total", Long.valueOf(settlementSettledTotal));
        payload.put("settlement_reconciliation_matched_total", Long.valueOf(settlementReconciliationMatchedTotal));
        payload.put("settlement_reconciliation_exception_total", Long.valueOf(settlementReconciliationExceptionTotal));
        payload.put("settlement_completion_rate", Double.valueOf(settlementCompletionRate));
        payload.put("settlement_reconciliation_accuracy_rate", Double.valueOf(settlementReconciliationAccuracyRate));
        return payload;
    }

    public Map<String, Object> buildGrowthMetrics() {
        return growthService.buildGrowthMetrics();
    }
}
