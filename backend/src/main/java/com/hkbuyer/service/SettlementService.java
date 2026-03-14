package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.domain.BuyerAuditStatus;
import com.hkbuyer.domain.BuyerProfile;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.SettlementLedger;
import com.hkbuyer.repository.BuyerRepository;
import com.hkbuyer.repository.SettlementRepository;
import com.hkbuyer.repository.TaskRepository;
import com.hkbuyer.repository.TimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private static final String SETTLEMENT_PENDING = "PENDING";
    private static final String SETTLEMENT_PAYOUT_REQUESTED = "PAYOUT_REQUESTED";
    private static final String SETTLEMENT_SETTLED = "SETTLED";

    private static final String RECONCILIATION_PENDING = "PENDING";
    private static final String RECONCILIATION_MATCHED = "MATCHED";
    private static final String RECONCILIATION_EXCEPTION = "EXCEPTION";

    private static final int BUYER_LEDGER_LIMIT = 200;
    private static final int PENDING_PAYOUT_LIMIT = 200;

    private static final BigDecimal GOODS_COST_RATE = new BigDecimal("0.70");
    private static final BigDecimal BUYER_INCOME_RATE = new BigDecimal("0.15");
    private static final BigDecimal LOGISTICS_COST_RATE = new BigDecimal("0.08");

    private final SettlementRepository settlementRepository;
    private final TaskRepository taskRepository;
    private final BuyerRepository buyerRepository;
    private final OrderService orderService;
    private final TimelineRepository timelineRepository;

    public SettlementService(SettlementRepository settlementRepository,
                             TaskRepository taskRepository,
                             BuyerRepository buyerRepository,
                             OrderService orderService,
                             TimelineRepository timelineRepository) {
        this.settlementRepository = settlementRepository;
        this.taskRepository = taskRepository;
        this.buyerRepository = buyerRepository;
        this.orderService = orderService;
        this.timelineRepository = timelineRepository;
    }

    @Transactional
    public Map<String, Object> ensureLedgerForSignedOrder(Long orderId, LocalDateTime signedAt) {
        Optional<SettlementLedger> existing = settlementRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return toLedgerPayload(existing.get());
        }

        OrderMain order = orderService.getOrderOrThrow(orderId);
        if (order.getOrderStatus() != OrderStatus.SIGNED) {
            throw new ApiException("order is not signed, settlement ledger cannot be generated");
        }

        ProcurementTask task = taskRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException("procurement task not found for order: " + orderId));
        if (task.getBuyerId() == null) {
            throw new ApiException("buyer not found for order task, settlement ledger cannot be generated");
        }

        BuyerProfile buyerProfile = buyerRepository.findProfileByBuyerId(task.getBuyerId())
                .orElseThrow(() -> new ApiException("buyer profile not found: " + task.getBuyerId()));
        if (buyerProfile.getAuditStatus() != BuyerAuditStatus.APPROVED) {
            throw new ApiException("buyer profile is not approved: " + task.getBuyerId());
        }

        SplitAmount splitAmount = buildSplitAmount(order.getTotalAmount());

        SettlementLedger ledger = new SettlementLedger();
        ledger.setOrderId(orderId);
        ledger.setTaskId(task.getTaskId());
        ledger.setBuyerId(task.getBuyerId());
        ledger.setBuyerSettlementAccount(trimToNull(buyerProfile.getSettlementAccount()));
        ledger.setOrderAmount(splitAmount.getOrderAmount());
        ledger.setGoodsCostAmount(splitAmount.getGoodsCostAmount());
        ledger.setBuyerIncomeAmount(splitAmount.getBuyerIncomeAmount());
        ledger.setLogisticsCostAmount(splitAmount.getLogisticsCostAmount());
        ledger.setPlatformServiceAmount(splitAmount.getPlatformServiceAmount());
        ledger.setSettlementStatus(SETTLEMENT_PENDING);
        ledger.setReconciliationStatus(RECONCILIATION_PENDING);
        ledger.setExceptionReason(null);
        ledger.setSignedAt(signedAt == null ? LocalDateTime.now() : signedAt);
        ledger.setPayoutRequestedAt(null);
        ledger.setSettledAt(null);
        ledger.setReconciledAt(null);

        long ledgerId = settlementRepository.createLedger(ledger);
        timelineRepository.addEvent(
                orderId,
                "settlement_ledger_generated",
                String.format(Locale.ROOT,
                        "订单签收后生成分账台账，ledgerId=%d，买手收益=%.2f，平台服务费=%.2f",
                        Long.valueOf(ledgerId),
                        splitAmount.getBuyerIncomeAmount(),
                        splitAmount.getPlatformServiceAmount())
        );

        return settlementRepository.findById(ledgerId)
                .map(this::toLedgerPayload)
                .orElseThrow(() -> new ApiException("settlement ledger not found after create: " + ledgerId));
    }

    public List<Map<String, Object>> listBuyerLedgers(Long buyerId, String settlementStatus) {
        buyerRepository.findProfileByBuyerId(buyerId)
                .orElseThrow(() -> new ApiException("buyer profile not found: " + buyerId));
        String normalizedSettlementStatus = normalizeSettlementStatusForFilter(settlementStatus);
        return settlementRepository
                .listByBuyerId(buyerId, normalizedSettlementStatus, BUYER_LEDGER_LIMIT)
                .stream()
                .map(this::toLedgerPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> requestPayout(Long ledgerId, Long buyerId) {
        SettlementLedger ledger = settlementRepository.findById(ledgerId)
                .orElseThrow(() -> new ApiException("settlement ledger not found: " + ledgerId));
        if (!ledger.getBuyerId().equals(buyerId)) {
            throw new ApiException("ledger does not belong to buyer");
        }

        int affected = settlementRepository.markPayoutRequested(ledgerId, LocalDateTime.now());
        if (affected == 0) {
            throw new ApiException("ledger is not in PENDING status");
        }

        timelineRepository.addEvent(
                ledger.getOrderId(),
                "settlement_payout_requested",
                "买手已发起结算申请，ledgerId=" + ledgerId
        );

        return settlementRepository.findById(ledgerId)
                .map(this::toLedgerPayload)
                .orElseThrow(() -> new ApiException("settlement ledger not found after payout request: " + ledgerId));
    }

    public List<Map<String, Object>> listPendingPayoutLedgers() {
        return settlementRepository.listPendingPayout(PENDING_PAYOUT_LIMIT)
                .stream()
                .map(this::toLedgerPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> completePayout(Long ledgerId, Long adminId, String comment) {
        SettlementLedger ledger = settlementRepository.findById(ledgerId)
                .orElseThrow(() -> new ApiException("settlement ledger not found: " + ledgerId));

        int affected = settlementRepository.markSettled(ledgerId, LocalDateTime.now());
        if (affected == 0) {
            throw new ApiException("ledger is not in PAYOUT_REQUESTED status");
        }

        String normalizedComment = trimToNull(comment);
        timelineRepository.addEvent(
                ledger.getOrderId(),
                "settlement_payout_completed",
                "后台已确认放款，ledgerId=" + ledgerId
                        + "，adminId=" + adminId
                        + (normalizedComment == null ? "" : "，备注: " + normalizedComment)
        );

        return settlementRepository.findById(ledgerId)
                .map(this::toLedgerPayload)
                .orElseThrow(() -> new ApiException("settlement ledger not found after payout complete: " + ledgerId));
    }

    @Transactional
    public Map<String, Object> reconcileLedger(Long ledgerId,
                                               Long adminId,
                                               String decision,
                                               String exceptionReason) {
        SettlementLedger ledger = settlementRepository.findById(ledgerId)
                .orElseThrow(() -> new ApiException("settlement ledger not found: " + ledgerId));
        if (!SETTLEMENT_SETTLED.equals(ledger.getSettlementStatus())) {
            throw new ApiException("only SETTLED ledger can be reconciled");
        }

        String normalizedDecision = normalizeReconciliationDecision(decision);
        String normalizedExceptionReason = trimToNull(exceptionReason);
        if (RECONCILIATION_EXCEPTION.equals(normalizedDecision) && normalizedExceptionReason == null) {
            throw new ApiException("exceptionReason must not be blank when decision is EXCEPTION");
        }
        if (RECONCILIATION_MATCHED.equals(normalizedDecision)) {
            normalizedExceptionReason = null;
        }

        int affected = settlementRepository.markReconciliation(
                ledgerId,
                normalizedDecision,
                normalizedExceptionReason,
                LocalDateTime.now()
        );
        if (affected == 0) {
            throw new ApiException("ledger reconciliation update failed");
        }

        timelineRepository.addEvent(
                ledger.getOrderId(),
                RECONCILIATION_MATCHED.equals(normalizedDecision)
                        ? "settlement_reconciliation_matched"
                        : "settlement_reconciliation_exception",
                "台账对账已提交，ledgerId=" + ledgerId
                        + "，adminId=" + adminId
                        + "，结果=" + normalizedDecision
                        + (normalizedExceptionReason == null ? "" : "，原因: " + normalizedExceptionReason)
        );

        return settlementRepository.findById(ledgerId)
                .map(this::toLedgerPayload)
                .orElseThrow(() -> new ApiException("settlement ledger not found after reconcile: " + ledgerId));
    }

    public Map<String, Object> buildReconciliationReport() {
        long totalLedgers = countTotalLedgers();
        long pendingLedgers = countPendingLedgers();
        long payoutRequestedLedgers = countPayoutRequestedLedgers();
        long settledLedgers = countSettledLedgers();
        long matchedLedgers = countReconciliationMatchedLedgers();
        long exceptionLedgers = countReconciliationExceptionLedgers();

        long reconciledLedgers = matchedLedgers + exceptionLedgers;

        double payoutCompletionRate = 0D;
        if (totalLedgers > 0L) {
            payoutCompletionRate = (settledLedgers * 1.0D) / totalLedgers;
        }

        double reconciliationCoverageRate = 0D;
        if (settledLedgers > 0L) {
            reconciliationCoverageRate = (reconciledLedgers * 1.0D) / settledLedgers;
        }

        double reconciliationAccuracyRate = 0D;
        if (reconciledLedgers > 0L) {
            reconciliationAccuracyRate = (matchedLedgers * 1.0D) / reconciledLedgers;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("settlement_ledger_total", Long.valueOf(totalLedgers));
        payload.put("settlement_pending_total", Long.valueOf(pendingLedgers));
        payload.put("settlement_payout_requested_total", Long.valueOf(payoutRequestedLedgers));
        payload.put("settlement_settled_total", Long.valueOf(settledLedgers));
        payload.put("settlement_reconciliation_matched_total", Long.valueOf(matchedLedgers));
        payload.put("settlement_reconciliation_exception_total", Long.valueOf(exceptionLedgers));
        payload.put("settlement_payout_completion_rate", Double.valueOf(payoutCompletionRate));
        payload.put("settlement_reconciliation_coverage_rate", Double.valueOf(reconciliationCoverageRate));
        payload.put("settlement_reconciliation_accuracy_rate", Double.valueOf(reconciliationAccuracyRate));
        payload.put("settlement_order_amount_total", settlementRepository.sumOrderAmount());
        payload.put("settlement_platform_service_amount_total", settlementRepository.sumPlatformServiceAmount());
        return payload;
    }

    public long countTotalLedgers() {
        return settlementRepository.countTotalLedgers();
    }

    public long countPendingLedgers() {
        return settlementRepository.countBySettlementStatus(SETTLEMENT_PENDING);
    }

    public long countPayoutRequestedLedgers() {
        return settlementRepository.countBySettlementStatus(SETTLEMENT_PAYOUT_REQUESTED);
    }

    public long countSettledLedgers() {
        return settlementRepository.countBySettlementStatus(SETTLEMENT_SETTLED);
    }

    public long countReconciliationMatchedLedgers() {
        return settlementRepository.countByReconciliationStatus(RECONCILIATION_MATCHED);
    }

    public long countReconciliationExceptionLedgers() {
        return settlementRepository.countByReconciliationStatus(RECONCILIATION_EXCEPTION);
    }

    private Map<String, Object> toLedgerPayload(SettlementLedger ledger) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ledgerId", ledger.getLedgerId());
        payload.put("orderId", ledger.getOrderId());
        payload.put("taskId", ledger.getTaskId());
        payload.put("buyerId", ledger.getBuyerId());
        payload.put("buyerSettlementAccount", ledger.getBuyerSettlementAccount());
        payload.put("orderAmount", ledger.getOrderAmount());
        payload.put("goodsCostAmount", ledger.getGoodsCostAmount());
        payload.put("buyerIncomeAmount", ledger.getBuyerIncomeAmount());
        payload.put("logisticsCostAmount", ledger.getLogisticsCostAmount());
        payload.put("platformServiceAmount", ledger.getPlatformServiceAmount());
        payload.put("settlementStatus", ledger.getSettlementStatus());
        payload.put("reconciliationStatus", ledger.getReconciliationStatus());
        payload.put("exceptionReason", ledger.getExceptionReason());
        payload.put("signedAt", ledger.getSignedAt());
        payload.put("payoutRequestedAt", ledger.getPayoutRequestedAt());
        payload.put("settledAt", ledger.getSettledAt());
        payload.put("reconciledAt", ledger.getReconciledAt());
        payload.put("createdAt", ledger.getCreatedAt());
        payload.put("updatedAt", ledger.getUpdatedAt());
        return payload;
    }

    private String normalizeSettlementStatusForFilter(String settlementStatus) {
        String value = trimToNull(settlementStatus);
        if (value == null) {
            return null;
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (SETTLEMENT_PENDING.equals(normalized)
                || SETTLEMENT_PAYOUT_REQUESTED.equals(normalized)
                || SETTLEMENT_SETTLED.equals(normalized)) {
            return normalized;
        }

        throw new ApiException("settlementStatus must be PENDING, PAYOUT_REQUESTED or SETTLED");
    }

    private String normalizeReconciliationDecision(String decision) {
        String value = trimToNull(decision);
        if (value == null) {
            throw new ApiException("decision must be MATCHED or EXCEPTION");
        }

        String normalized = value.toUpperCase(Locale.ROOT);
        if (RECONCILIATION_MATCHED.equals(normalized) || RECONCILIATION_EXCEPTION.equals(normalized)) {
            return normalized;
        }

        throw new ApiException("decision must be MATCHED or EXCEPTION");
    }

    private SplitAmount buildSplitAmount(BigDecimal totalAmount) {
        BigDecimal orderAmount = normalizeMoney(totalAmount);
        if (orderAmount.signum() <= 0) {
            throw new ApiException("order total amount must be greater than zero");
        }

        BigDecimal goodsCostAmount = normalizeMoney(orderAmount.multiply(GOODS_COST_RATE));
        BigDecimal buyerIncomeAmount = normalizeMoney(orderAmount.multiply(BUYER_INCOME_RATE));
        BigDecimal logisticsCostAmount = normalizeMoney(orderAmount.multiply(LOGISTICS_COST_RATE));

        BigDecimal platformServiceAmount = normalizeMoney(
                orderAmount.subtract(goodsCostAmount).subtract(buyerIncomeAmount).subtract(logisticsCostAmount)
        );

        if (platformServiceAmount.signum() < 0) {
            logisticsCostAmount = normalizeMoney(logisticsCostAmount.add(platformServiceAmount));
            platformServiceAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (logisticsCostAmount.signum() < 0) {
            buyerIncomeAmount = normalizeMoney(buyerIncomeAmount.add(logisticsCostAmount));
            logisticsCostAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (buyerIncomeAmount.signum() < 0) {
            goodsCostAmount = normalizeMoney(goodsCostAmount.add(buyerIncomeAmount));
            buyerIncomeAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (goodsCostAmount.signum() < 0) {
            throw new ApiException("split amount calculation failed");
        }

        platformServiceAmount = normalizeMoney(
                orderAmount.subtract(goodsCostAmount).subtract(buyerIncomeAmount).subtract(logisticsCostAmount)
        );

        return new SplitAmount(
                orderAmount,
                goodsCostAmount,
                buyerIncomeAmount,
                logisticsCostAmount,
                platformServiceAmount
        );
    }

    private BigDecimal normalizeMoney(BigDecimal input) {
        if (input == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return input.setScale(2, RoundingMode.HALF_UP);
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

    private static class SplitAmount {
        private final BigDecimal orderAmount;
        private final BigDecimal goodsCostAmount;
        private final BigDecimal buyerIncomeAmount;
        private final BigDecimal logisticsCostAmount;
        private final BigDecimal platformServiceAmount;

        private SplitAmount(BigDecimal orderAmount,
                            BigDecimal goodsCostAmount,
                            BigDecimal buyerIncomeAmount,
                            BigDecimal logisticsCostAmount,
                            BigDecimal platformServiceAmount) {
            this.orderAmount = orderAmount;
            this.goodsCostAmount = goodsCostAmount;
            this.buyerIncomeAmount = buyerIncomeAmount;
            this.logisticsCostAmount = logisticsCostAmount;
            this.platformServiceAmount = platformServiceAmount;
        }

        public BigDecimal getOrderAmount() {
            return orderAmount;
        }

        public BigDecimal getGoodsCostAmount() {
            return goodsCostAmount;
        }

        public BigDecimal getBuyerIncomeAmount() {
            return buyerIncomeAmount;
        }

        public BigDecimal getLogisticsCostAmount() {
            return logisticsCostAmount;
        }

        public BigDecimal getPlatformServiceAmount() {
            return platformServiceAmount;
        }
    }
}
