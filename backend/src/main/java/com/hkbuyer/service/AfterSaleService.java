package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.domain.AfterSaleCase;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.repository.AfterSaleRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AfterSaleService {

    private static final String CASE_TYPE_STOCKOUT = "STOCKOUT";
    private static final String CASE_TYPE_AUTHENTICITY = "AUTHENTICITY";

    private static final String CASE_STATUS_USER_PENDING = "USER_PENDING";
    private static final String CASE_STATUS_ADMIN_REVIEW = "ADMIN_REVIEW";
    private static final String CASE_STATUS_RESOLVED = "RESOLVED";
    private static final String CASE_STATUS_REJECTED = "REJECTED";

    private static final String USER_DECISION_ACCEPT_REPLACEMENT = "ACCEPT_REPLACEMENT";
    private static final String USER_DECISION_REQUEST_PARTIAL_REFUND = "REQUEST_PARTIAL_REFUND";

    private static final String ADMIN_DECISION_APPROVE_REPLACEMENT = "APPROVE_REPLACEMENT";
    private static final String ADMIN_DECISION_APPROVE_PARTIAL_REFUND = "APPROVE_PARTIAL_REFUND";
    private static final String ADMIN_DECISION_REJECT_CLAIM = "REJECT_CLAIM";

    private static final String RISK_LOW = "LOW";
    private static final String RISK_HIGH = "HIGH";

    private final AfterSaleRepository afterSaleRepository;
    private final TaskRepository taskRepository;
    private final OrderService orderService;

    public AfterSaleService(AfterSaleRepository afterSaleRepository,
                            TaskRepository taskRepository,
                            OrderService orderService) {
        this.afterSaleRepository = afterSaleRepository;
        this.taskRepository = taskRepository;
        this.orderService = orderService;
    }

    @Transactional
    public Map<String, Object> reportStockout(Long taskId,
                                              Long buyerId,
                                              String issueReason,
                                              String replacementSkuName,
                                              BigDecimal suggestedRefundAmount) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getBuyerId() == null || !task.getBuyerId().equals(buyerId)) {
            throw new ApiException("buyer does not match accepted task");
        }
        if (task.getTaskStatus() != TaskStatus.ACCEPTED && task.getTaskStatus() != TaskStatus.PROOF_REJECTED) {
            throw new ApiException("task is not ready for stockout report");
        }

        String normalizedReason = trimToNull(issueReason);
        if (normalizedReason == null) {
            throw new ApiException("issueReason must not be blank");
        }

        if (afterSaleRepository.countOpenStockoutCasesByTaskId(taskId) > 0L) {
            throw new ApiException("stockout case already exists for this task");
        }

        OrderMain order = orderService.getOrderOrThrow(task.getOrderId());
        BigDecimal normalizedSuggestedRefund = normalizeMoneyOrNull(suggestedRefundAmount);

        AfterSaleCase item = new AfterSaleCase();
        item.setOrderId(order.getOrderId());
        item.setTaskId(taskId);
        item.setUserId(order.getUserId());
        item.setBuyerId(buyerId);
        item.setCaseType(CASE_TYPE_STOCKOUT);
        item.setCaseStatus(CASE_STATUS_USER_PENDING);
        item.setIssueReason(normalizedReason);
        item.setReplacementSkuName(trimToNull(replacementSkuName));
        item.setSuggestedRefundAmount(normalizedSuggestedRefund);
        item.setRiskLevel(RISK_LOW);
        item.setOriginOrderStatus(resolveOriginOrderStatus(order.getOrderStatus()));

        long caseId = afterSaleRepository.createCase(item);
        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatus.AFTER_SALE_PROCESSING,
                "after_sale_stockout_reported",
                "买手上报缺货，等待用户选择替代或部分退款"
        );

        return getOrderCaseDetail(order.getOrderId(), caseId, order.getUserId());
    }

    @Transactional
    public Map<String, Object> createAuthenticityDispute(Long orderId,
                                                          Long userId,
                                                          String issueReason,
                                                          String evidenceUrl) {
        OrderMain order = orderService.getOrderOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException("order does not belong to user");
        }

        if (afterSaleRepository.countOpenAuthenticityCasesByOrderId(orderId) > 0L) {
            throw new ApiException("authenticity dispute already exists for this order");
        }

        String normalizedReason = trimToNull(issueReason);
        if (normalizedReason == null) {
            throw new ApiException("issueReason must not be blank");
        }

        AfterSaleCase item = new AfterSaleCase();
        item.setOrderId(orderId);
        item.setTaskId(null);
        item.setUserId(userId);
        item.setBuyerId(null);
        item.setCaseType(CASE_TYPE_AUTHENTICITY);
        item.setCaseStatus(CASE_STATUS_ADMIN_REVIEW);
        item.setIssueReason(normalizedReason);
        item.setEvidenceUrl(trimToNull(evidenceUrl));
        item.setRiskLevel(RISK_HIGH);
        item.setOriginOrderStatus(resolveOriginOrderStatus(order.getOrderStatus()));

        long caseId = afterSaleRepository.createCase(item);

        orderService.updateOrderStatus(
                orderId,
                OrderStatus.AFTER_SALE_PROCESSING,
                "after_sale_authenticity_created",
                "用户发起真伪争议，待平台仲裁"
        );

        return getOrderCaseDetail(orderId, caseId, userId);
    }

    @Transactional
    public Map<String, Object> submitUserDecision(Long orderId,
                                                  Long caseId,
                                                  Long userId,
                                                  String decision,
                                                  String comment) {
        OrderMain order = orderService.getOrderOrThrow(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new ApiException("order does not belong to user");
        }

        AfterSaleCase item = afterSaleRepository.findById(caseId)
                .orElseThrow(() -> new ApiException("after sale case not found: " + caseId));
        if (!item.getOrderId().equals(orderId)) {
            throw new ApiException("case does not belong to order");
        }
        if (!item.getUserId().equals(userId)) {
            throw new ApiException("case does not belong to user");
        }
        if (!CASE_TYPE_STOCKOUT.equals(item.getCaseType())) {
            throw new ApiException("only stockout case supports user decision");
        }
        if (!CASE_STATUS_USER_PENDING.equals(item.getCaseStatus())) {
            throw new ApiException("case is not waiting for user decision");
        }

        String normalizedDecision = normalizeUserDecision(decision);
        String normalizedComment = trimToNull(comment);

        afterSaleRepository.markUserDecision(
                caseId,
                CASE_STATUS_ADMIN_REVIEW,
                normalizedDecision,
                normalizedComment,
                LocalDateTime.now()
        );

        orderService.updateOrderStatus(
                orderId,
                OrderStatus.AFTER_SALE_PROCESSING,
                "after_sale_user_decision_submitted",
                "用户已提交缺货方案决策，等待平台仲裁"
        );

        return getOrderCaseDetail(orderId, caseId, userId);
    }

    public List<Map<String, Object>> listOrderCases(Long orderId, Long userId) {
        OrderMain order = orderService.getOrderOrThrow(orderId);
        if (userId != null && !order.getUserId().equals(userId)) {
            throw new ApiException("order does not belong to user");
        }
        return afterSaleRepository.listByOrderId(orderId)
                .stream()
                .map(this::toCasePayload)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listPendingArbitrationCases() {
        return afterSaleRepository.listPendingArbitrationCases()
                .stream()
                .map(this::toCasePayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> arbitrateCase(Long caseId,
                                             Long adminId,
                                             String decision,
                                             String comment,
                                             BigDecimal finalRefundAmount) {
        AfterSaleCase item = afterSaleRepository.findById(caseId)
                .orElseThrow(() -> new ApiException("after sale case not found: " + caseId));
        if (!CASE_STATUS_ADMIN_REVIEW.equals(item.getCaseStatus())) {
            throw new ApiException("case is not in ADMIN_REVIEW status");
        }

        String normalizedDecision = normalizeAdminDecision(decision);
        String normalizedComment = trimToNull(comment);

        String caseStatus = CASE_STATUS_RESOLVED;
        BigDecimal negotiatedRefundAmount = null;
        if (ADMIN_DECISION_APPROVE_PARTIAL_REFUND.equals(normalizedDecision)) {
            negotiatedRefundAmount = normalizeMoneyOrNull(finalRefundAmount);
            if (negotiatedRefundAmount == null) {
                negotiatedRefundAmount = normalizeMoneyOrNull(item.getSuggestedRefundAmount());
            }
            if (negotiatedRefundAmount == null || negotiatedRefundAmount.signum() <= 0) {
                throw new ApiException("finalRefundAmount must be greater than zero for partial refund decision");
            }
        }

        if (ADMIN_DECISION_REJECT_CLAIM.equals(normalizedDecision)) {
            caseStatus = CASE_STATUS_REJECTED;
        }

        LocalDateTime now = LocalDateTime.now();
        afterSaleRepository.markArbitration(
                caseId,
                caseStatus,
                normalizedDecision,
                adminId,
                normalizedComment,
                negotiatedRefundAmount,
                now,
                now
        );

        long openCases = afterSaleRepository.countOpenCasesByOrderId(item.getOrderId());
        if (openCases == 0L) {
            OrderStatus restoreStatus = parseOrderStatusOrDefault(item.getOriginOrderStatus());
            orderService.updateOrderStatus(
                    item.getOrderId(),
                    restoreStatus,
                    "after_sale_case_closed",
                    "售后仲裁已结案，结论: " + normalizedDecision
            );
        } else {
            orderService.updateOrderStatus(
                    item.getOrderId(),
                    OrderStatus.AFTER_SALE_PROCESSING,
                    "after_sale_case_arbitrated",
                    "当前工单已仲裁，订单仍有其他待处理售后工单"
            );
        }

        return getOrderCaseDetail(item.getOrderId(), caseId, item.getUserId());
    }

    public long countOpenCases() {
        return afterSaleRepository.countOpenCases();
    }

    public long countPendingArbitrationCases() {
        return afterSaleRepository.countPendingArbitrationCases();
    }

    public long countResolvedCases() {
        return afterSaleRepository.countResolvedCases();
    }

    public long countCounterfeitDisputes() {
        return afterSaleRepository.countCounterfeitDisputes();
    }

    public long countApprovedPartialRefundCases() {
        return afterSaleRepository.countApprovedPartialRefundCases();
    }

    private Map<String, Object> getOrderCaseDetail(Long orderId, Long caseId, Long userId) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("caseId", caseId);
        payload.put("userId", userId);
        payload.put("cases", listOrderCases(orderId, userId));
        return payload;
    }

    private Map<String, Object> toCasePayload(AfterSaleCase item) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("caseId", item.getCaseId());
        payload.put("orderId", item.getOrderId());
        payload.put("taskId", item.getTaskId());
        payload.put("userId", item.getUserId());
        payload.put("buyerId", item.getBuyerId());
        payload.put("caseType", item.getCaseType());
        payload.put("caseStatus", item.getCaseStatus());
        payload.put("issueReason", item.getIssueReason());
        payload.put("evidenceUrl", item.getEvidenceUrl());
        payload.put("replacementSkuName", item.getReplacementSkuName());
        payload.put("suggestedRefundAmount", item.getSuggestedRefundAmount());
        payload.put("negotiatedRefundAmount", item.getNegotiatedRefundAmount());
        payload.put("userDecision", item.getUserDecision());
        payload.put("userComment", item.getUserComment());
        payload.put("arbitrationResult", item.getArbitrationResult());
        payload.put("riskLevel", item.getRiskLevel());
        payload.put("originOrderStatus", item.getOriginOrderStatus());
        payload.put("adminId", item.getAdminId());
        payload.put("arbitrationComment", item.getArbitrationComment());
        payload.put("createdAt", item.getCreatedAt());
        payload.put("userDecisionAt", item.getUserDecisionAt());
        payload.put("arbitratedAt", item.getArbitratedAt());
        payload.put("closedAt", item.getClosedAt());
        payload.put("updatedAt", item.getUpdatedAt());
        return payload;
    }

    private String normalizeUserDecision(String decision) {
        String value = trimToNull(decision);
        if (value == null) {
            throw new ApiException("decision must be ACCEPT_REPLACEMENT or REQUEST_PARTIAL_REFUND");
        }
        String normalized = value.toUpperCase();
        if (USER_DECISION_ACCEPT_REPLACEMENT.equals(normalized)
                || USER_DECISION_REQUEST_PARTIAL_REFUND.equals(normalized)) {
            return normalized;
        }
        throw new ApiException("decision must be ACCEPT_REPLACEMENT or REQUEST_PARTIAL_REFUND");
    }

    private String normalizeAdminDecision(String decision) {
        String value = trimToNull(decision);
        if (value == null) {
            throw new ApiException("decision must be APPROVE_REPLACEMENT, APPROVE_PARTIAL_REFUND or REJECT_CLAIM");
        }
        String normalized = value.toUpperCase();
        if (ADMIN_DECISION_APPROVE_REPLACEMENT.equals(normalized)
                || ADMIN_DECISION_APPROVE_PARTIAL_REFUND.equals(normalized)
                || ADMIN_DECISION_REJECT_CLAIM.equals(normalized)) {
            return normalized;
        }
        throw new ApiException("decision must be APPROVE_REPLACEMENT, APPROVE_PARTIAL_REFUND or REJECT_CLAIM");
    }

    private BigDecimal normalizeMoneyOrNull(BigDecimal input) {
        if (input == null) {
            return null;
        }
        return input.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveOriginOrderStatus(OrderStatus orderStatus) {
        if (orderStatus == null || orderStatus == OrderStatus.AFTER_SALE_PROCESSING) {
            return OrderStatus.BUYER_PROCUREMENT.name();
        }
        return orderStatus.name();
    }

    private OrderStatus parseOrderStatusOrDefault(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return OrderStatus.BUYER_PROCUREMENT;
        }
        try {
            OrderStatus parsed = OrderStatus.valueOf(value);
            if (parsed == OrderStatus.AFTER_SALE_PROCESSING) {
                return OrderStatus.BUYER_PROCUREMENT;
            }
            return parsed;
        } catch (IllegalArgumentException ex) {
            return OrderStatus.BUYER_PROCUREMENT;
        }
    }

    private String trimToNull(String input) {
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            return null;
        }
        return value;
    }
}
