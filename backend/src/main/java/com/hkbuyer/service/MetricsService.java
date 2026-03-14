package com.hkbuyer.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MetricsService {

    private final OrderService orderService;
    private final TaskService taskService;
    private final ProofService proofService;

    public MetricsService(OrderService orderService,
                          TaskService taskService,
                          ProofService proofService) {
        this.orderService = orderService;
        this.taskService = taskService;
        this.proofService = proofService;
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
}
