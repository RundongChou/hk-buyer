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

    public MetricsService(OrderService orderService,
                          TaskService taskService,
                          ProofService proofService,
                          BuyerService buyerService) {
        this.orderService = orderService;
        this.taskService = taskService;
        this.proofService = proofService;
        this.buyerService = buyerService;
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
}
