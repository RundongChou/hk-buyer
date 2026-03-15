package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.CreateOpsExperimentRequest;
import com.hkbuyer.domain.OpsAlertEvent;
import com.hkbuyer.domain.OpsExperiment;
import com.hkbuyer.domain.OpsExperimentAssignment;
import com.hkbuyer.domain.OpsVariant;
import com.hkbuyer.repository.AfterSaleRepository;
import com.hkbuyer.repository.FulfillmentRepository;
import com.hkbuyer.repository.OptimizationRepository;
import com.hkbuyer.repository.OrderRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
public class OptimizationService {

    private static final BigDecimal ONE = new BigDecimal("1.0000");
    private static final BigDecimal TIMEOUT_UNACCEPTED_THRESHOLD = new BigDecimal("0.1500");
    private static final BigDecimal ORDER_CANCEL_THRESHOLD = new BigDecimal("0.0800");
    private static final BigDecimal SIGNED_IN_7_15_THRESHOLD = new BigDecimal("0.9000");
    private static final BigDecimal CLEARANCE_SUCCESS_THRESHOLD = new BigDecimal("0.9950");
    private static final BigDecimal COUNTERFEIT_COMPLAINT_THRESHOLD = new BigDecimal("0.0030");

    private final OptimizationRepository optimizationRepository;
    private final TaskRepository taskRepository;
    private final OrderRepository orderRepository;
    private final FulfillmentRepository fulfillmentRepository;
    private final AfterSaleRepository afterSaleRepository;

    public OptimizationService(OptimizationRepository optimizationRepository,
                               TaskRepository taskRepository,
                               OrderRepository orderRepository,
                               FulfillmentRepository fulfillmentRepository,
                               AfterSaleRepository afterSaleRepository) {
        this.optimizationRepository = optimizationRepository;
        this.taskRepository = taskRepository;
        this.orderRepository = orderRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.afterSaleRepository = afterSaleRepository;
    }

    @Transactional
    public Map<String, Object> createExperiment(CreateOpsExperimentRequest request) {
        BigDecimal controlRatio = normalizeRatio(request.getControlRatio());
        BigDecimal treatmentRatio = normalizeRatio(request.getTreatmentRatio());
        BigDecimal sumRatio = controlRatio.add(treatmentRatio).setScale(4, RoundingMode.HALF_UP);
        if (sumRatio.compareTo(ONE) != 0) {
            throw new ApiException("controlRatio + treatmentRatio must be 1.0000");
        }

        OpsExperiment item = new OpsExperiment();
        item.setExperimentKey(normalizeExperimentKey(request.getExperimentKey()));
        item.setExperimentName(request.getExperimentName().trim());
        item.setExperimentStatus("DRAFT");
        item.setControlRatio(controlRatio);
        item.setTreatmentRatio(treatmentRatio);
        item.setTreatmentMarkupDelta(normalizeMoney(request.getTreatmentMarkupDelta()));
        item.setTreatmentSlaHours(Integer.valueOf(clampSlaHours(request.getTreatmentSlaHours())));
        item.setCreatedBy(request.getAdminId());

        long experimentId;
        try {
            experimentId = optimizationRepository.createExperiment(item);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException("experimentKey already exists");
        }

        return getExperimentPayloadById(Long.valueOf(experimentId));
    }

    @Transactional
    public Map<String, Object> activateExperiment(Long experimentId, Long adminId) {
        OpsExperiment experiment = optimizationRepository.findExperimentById(experimentId)
                .orElseThrow(() -> new ApiException("experiment not found: " + experimentId));

        optimizationRepository.pauseActiveExperiments();
        int affected = optimizationRepository.activateExperiment(experimentId, adminId, LocalDateTime.now());
        if (affected == 0) {
            throw new ApiException("failed to activate experiment: " + experimentId);
        }

        Map<String, Object> payload = getExperimentPayloadById(experimentId);
        payload.put("previousStatus", experiment.getExperimentStatus());
        payload.put("activatedBy", adminId);
        payload.put("activationResult", "ACTIVE");
        return payload;
    }

    public Map<String, Object> getActiveExperiment() {
        Optional<OpsExperiment> optional = optimizationRepository.findActiveExperiment();
        if (!optional.isPresent()) {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("hasActiveExperiment", Boolean.FALSE);
            return payload;
        }

        Map<String, Object> payload = toExperimentPayload(optional.get());
        payload.put("hasActiveExperiment", Boolean.TRUE);
        return payload;
    }

    public List<Map<String, Object>> listAssignments(Long experimentId, int limit) {
        optimizationRepository.findExperimentById(experimentId)
                .orElseThrow(() -> new ApiException("experiment not found: " + experimentId));

        int normalizedLimit = limit <= 0 ? 200 : Math.min(limit, 500);
        return optimizationRepository.listAssignmentsByExperiment(experimentId, normalizedLimit)
                .stream()
                .map(this::toAssignmentPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public DispatchOptimizationDecision optimizeDispatch(Long orderId,
                                                         Long userId,
                                                         BigDecimal baseMarkup,
                                                         Integer baseSlaHours) {
        BigDecimal normalizedBaseMarkup = normalizeMoney(baseMarkup);
        int normalizedBaseSlaHours = clampSlaHours(baseSlaHours);

        Optional<OpsExperiment> activeOptional = optimizationRepository.findActiveExperiment();
        if (!activeOptional.isPresent()) {
            return DispatchOptimizationDecision.baseline(normalizedBaseMarkup, normalizedBaseSlaHours);
        }

        OpsExperiment experiment = activeOptional.get();
        Optional<OpsExperimentAssignment> existingAssignment = optimizationRepository.findAssignment(
                experiment.getExperimentId(),
                orderId
        );
        if (existingAssignment.isPresent()) {
            OpsExperimentAssignment item = existingAssignment.get();
            return DispatchOptimizationDecision.withExperiment(
                    item.getExperimentId(),
                    item.getVariant(),
                    item.getFinalMarkup(),
                    item.getFinalSlaHours()
            );
        }

        OpsVariant variant = decideVariant(experiment, userId, orderId);
        BigDecimal finalMarkup = normalizedBaseMarkup;
        int finalSlaHours = normalizedBaseSlaHours;

        if (variant == OpsVariant.TREATMENT) {
            finalMarkup = normalizeMoney(normalizedBaseMarkup.add(normalizeMoney(experiment.getTreatmentMarkupDelta())));
            if (finalMarkup.signum() < 0) {
                finalMarkup = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            finalSlaHours = clampSlaHours(experiment.getTreatmentSlaHours());
        }

        OpsExperimentAssignment assignment = new OpsExperimentAssignment();
        assignment.setExperimentId(experiment.getExperimentId());
        assignment.setOrderId(orderId);
        assignment.setUserId(userId);
        assignment.setVariant(variant);
        assignment.setBaseSlaHours(Integer.valueOf(normalizedBaseSlaHours));
        assignment.setFinalSlaHours(Integer.valueOf(finalSlaHours));
        assignment.setBaseMarkup(normalizedBaseMarkup);
        assignment.setFinalMarkup(finalMarkup);
        assignment.setAssignedAt(LocalDateTime.now());

        try {
            optimizationRepository.createAssignment(assignment);
        } catch (DataIntegrityViolationException ex) {
            // Idempotent fallback for concurrent retries.
        }

        Optional<OpsExperimentAssignment> latestAssignment = optimizationRepository.findAssignment(
                experiment.getExperimentId(),
                orderId
        );
        if (latestAssignment.isPresent()) {
            OpsExperimentAssignment item = latestAssignment.get();
            return DispatchOptimizationDecision.withExperiment(
                    item.getExperimentId(),
                    item.getVariant(),
                    item.getFinalMarkup(),
                    item.getFinalSlaHours()
            );
        }

        return DispatchOptimizationDecision.withExperiment(
                experiment.getExperimentId(),
                variant,
                finalMarkup,
                Integer.valueOf(finalSlaHours)
        );
    }

    @Transactional
    public Map<String, Object> evaluateAlerts() {
        long totalTasks = taskRepository.countAllTasks();
        long timeoutUnacceptedTasks = taskRepository.countTimeoutUnacceptedTasks();
        BigDecimal timeoutUnacceptedRate = ratio(timeoutUnacceptedTasks, totalTasks, 4, BigDecimal.ZERO);

        long totalOrders = orderRepository.countTotalOrders();
        long cancelledOrders = orderRepository.countCancelledOrders();
        BigDecimal orderCancelRate = ratio(cancelledOrders, totalOrders, 4, BigDecimal.ZERO);

        long signedOrders = fulfillmentRepository.countShipmentSigned();
        long signedWithinSevenToFifteenDays = fulfillmentRepository.countSignedWithinSevenToFifteenDays();
        BigDecimal signedWithinSevenToFifteenRate = ratio(
                signedWithinSevenToFifteenDays,
                signedOrders,
                4,
                BigDecimal.ZERO
        );

        long customsSubmitted = fulfillmentRepository.countCustomsSubmitted();
        long customsReleased = fulfillmentRepository.countCustomsReleased();
        BigDecimal complianceClearanceSuccessRate = ratio(customsReleased, customsSubmitted, 4, BigDecimal.ONE);

        long paidOrders = orderRepository.countPaidOrders();
        long counterfeitDisputes = afterSaleRepository.countCounterfeitDisputes();
        BigDecimal counterfeitComplaintRate = ratio(counterfeitDisputes, paidOrders, 4, BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        int createdCount = 0;
        int resolvedCount = 0;

        AlertOutcome timeoutOutcome = evaluateThreshold(
                "TIMEOUT_UNACCEPTED_RATE",
                "task_timeout_unaccepted_72h_rate",
                timeoutUnacceptedRate,
                TIMEOUT_UNACCEPTED_THRESHOLD,
                true,
                "HIGH",
                now,
                "72h 超时未接单率超阈"
        );
        createdCount += timeoutOutcome.created;
        resolvedCount += timeoutOutcome.resolved;

        AlertOutcome cancelOutcome = evaluateThreshold(
                "ORDER_CANCEL_RATE",
                "order_cancel_rate",
                orderCancelRate,
                ORDER_CANCEL_THRESHOLD,
                true,
                "HIGH",
                now,
                "订单取消率超阈"
        );
        createdCount += cancelOutcome.created;
        resolvedCount += cancelOutcome.resolved;

        AlertOutcome signedRateOutcome = evaluateThreshold(
                "SIGNED_IN_7_15_RATE",
                "signed_within_7_15_days_rate",
                signedWithinSevenToFifteenRate,
                SIGNED_IN_7_15_THRESHOLD,
                false,
                "HIGH",
                now,
                "7-15天履约达成率低于阈值"
        );
        createdCount += signedRateOutcome.created;
        resolvedCount += signedRateOutcome.resolved;

        AlertOutcome clearanceOutcome = evaluateThreshold(
                "COMPLIANCE_CLEARANCE_SUCCESS_RATE",
                "compliance_clearance_success_rate",
                complianceClearanceSuccessRate,
                CLEARANCE_SUCCESS_THRESHOLD,
                false,
                "HIGH",
                now,
                "合规清关成功率低于阈值"
        );
        createdCount += clearanceOutcome.created;
        resolvedCount += clearanceOutcome.resolved;

        AlertOutcome counterfeitOutcome = evaluateThreshold(
                "COUNTERFEIT_COMPLAINT_RATE",
                "counterfeit_complaint_rate",
                counterfeitComplaintRate,
                COUNTERFEIT_COMPLAINT_THRESHOLD,
                true,
                "HIGH",
                now,
                "假货投诉率超阈"
        );
        createdCount += counterfeitOutcome.created;
        resolvedCount += counterfeitOutcome.resolved;

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("evaluatedAt", now);
        payload.put("createdAlerts", Integer.valueOf(createdCount));
        payload.put("resolvedAlerts", Integer.valueOf(resolvedCount));
        payload.put("openAlertTotal", Long.valueOf(optimizationRepository.countOpenAlerts()));
        payload.put("openHighAlertTotal", Long.valueOf(optimizationRepository.countOpenAlertsBySeverity("HIGH")));

        Map<String, Object> snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("task_timeout_unaccepted_72h_rate", timeoutUnacceptedRate);
        snapshot.put("order_cancel_rate", orderCancelRate);
        snapshot.put("signed_within_7_15_days_rate", signedWithinSevenToFifteenRate);
        snapshot.put("compliance_clearance_success_rate", complianceClearanceSuccessRate);
        snapshot.put("counterfeit_complaint_rate", counterfeitComplaintRate);
        payload.put("guardrailSnapshot", snapshot);
        return payload;
    }

    public List<Map<String, Object>> listOpenAlerts(int limit) {
        int normalizedLimit = limit <= 0 ? 200 : Math.min(limit, 500);
        return optimizationRepository.listOpenAlerts(normalizedLimit)
                .stream()
                .map(this::toAlertPayload)
                .collect(Collectors.toList());
    }

    public Map<String, Object> buildOptimizationMetrics() {
        long experimentTotal = optimizationRepository.countExperiments();
        long activeExperimentTotal = optimizationRepository.countActiveExperiments();

        long assignmentTotal = optimizationRepository.countAssignments();
        long controlAssignments = optimizationRepository.countAssignmentsByVariant(OpsVariant.CONTROL);
        long treatmentAssignments = optimizationRepository.countAssignmentsByVariant(OpsVariant.TREATMENT);

        long controlSignedTotal = optimizationRepository.countSignedAssignmentsByVariant(OpsVariant.CONTROL);
        long controlSignedInTarget = optimizationRepository.countSignedWithinSevenToFifteenByVariant(OpsVariant.CONTROL);
        BigDecimal controlSignedRate = ratio(controlSignedInTarget, controlSignedTotal, 4, BigDecimal.ZERO);

        long treatmentSignedTotal = optimizationRepository.countSignedAssignmentsByVariant(OpsVariant.TREATMENT);
        long treatmentSignedInTarget = optimizationRepository.countSignedWithinSevenToFifteenByVariant(OpsVariant.TREATMENT);
        BigDecimal treatmentSignedRate = ratio(treatmentSignedInTarget, treatmentSignedTotal, 4, BigDecimal.ZERO);

        BigDecimal controlCostRate = optimizationRepository.avgPlatformServiceRateByVariant(OpsVariant.CONTROL)
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal treatmentCostRate = optimizationRepository.avgPlatformServiceRateByVariant(OpsVariant.TREATMENT)
                .setScale(4, RoundingMode.HALF_UP);

        long openAlertTotal = optimizationRepository.countOpenAlerts();
        long openHighAlertTotal = optimizationRepository.countOpenAlertsBySeverity("HIGH");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("ops_experiment_total", Long.valueOf(experimentTotal));
        payload.put("ops_experiment_active_total", Long.valueOf(activeExperimentTotal));
        payload.put("ops_assignment_total", Long.valueOf(assignmentTotal));
        payload.put("ops_assignment_control_total", Long.valueOf(controlAssignments));
        payload.put("ops_assignment_treatment_total", Long.valueOf(treatmentAssignments));
        payload.put("ops_control_signed_in_7_15_rate", controlSignedRate.doubleValue());
        payload.put("ops_treatment_signed_in_7_15_rate", treatmentSignedRate.doubleValue());
        payload.put("ops_control_platform_service_rate", controlCostRate.doubleValue());
        payload.put("ops_treatment_platform_service_rate", treatmentCostRate.doubleValue());
        payload.put("ops_open_alert_total", Long.valueOf(openAlertTotal));
        payload.put("ops_open_high_alert_total", Long.valueOf(openHighAlertTotal));
        payload.put("ops_strategy_recommendation", buildRecommendation(
                openHighAlertTotal,
                treatmentAssignments,
                controlSignedRate,
                treatmentSignedRate,
                controlCostRate,
                treatmentCostRate
        ));
        return payload;
    }

    private Map<String, Object> getExperimentPayloadById(Long experimentId) {
        OpsExperiment saved = optimizationRepository.findExperimentById(experimentId)
                .orElseThrow(() -> new ApiException("experiment not found after save: " + experimentId));
        return toExperimentPayload(saved);
    }

    private String buildRecommendation(long openHighAlerts,
                                       long treatmentAssignments,
                                       BigDecimal controlSignedRate,
                                       BigDecimal treatmentSignedRate,
                                       BigDecimal controlCostRate,
                                       BigDecimal treatmentCostRate) {
        if (openHighAlerts > 0L) {
            return "PAUSE_EXPERIMENT_AND_HANDLE_ALERTS";
        }
        if (treatmentAssignments < 30L) {
            return "COLLECT_MORE_SAMPLES";
        }
        if (treatmentSignedRate.compareTo(controlSignedRate) >= 0
                && treatmentCostRate.compareTo(controlCostRate) <= 0) {
            return "ROLL_OUT_TREATMENT";
        }

        BigDecimal safetyGap = controlSignedRate.subtract(new BigDecimal("0.0200"));
        if (treatmentSignedRate.compareTo(safetyGap) < 0) {
            return "ROLLBACK_TO_CONTROL";
        }
        return "KEEP_EXPERIMENT_RUNNING";
    }

    private AlertOutcome evaluateThreshold(String alertType,
                                           String metricKey,
                                           BigDecimal metricValue,
                                           BigDecimal threshold,
                                           boolean breachWhenGreater,
                                           String severity,
                                           LocalDateTime now,
                                           String messagePrefix) {
        boolean breached = breachWhenGreater
                ? metricValue.compareTo(threshold) > 0
                : metricValue.compareTo(threshold) < 0;

        int created = 0;
        int resolved = 0;
        if (breached) {
            if (optimizationRepository.countOpenAlertsByType(alertType) == 0L) {
                String detail = String.format(Locale.ROOT,
                        "%s: metric=%s, threshold=%s",
                        messagePrefix,
                        metricValue.toPlainString(),
                        threshold.toPlainString());
                optimizationRepository.createAlert(
                        alertType,
                        metricKey,
                        metricValue,
                        threshold,
                        severity,
                        detail,
                        now
                );
                created = 1;
            }
        } else {
            resolved = optimizationRepository.resolveOpenAlertsByType(
                    alertType,
                    "指标恢复到阈值内",
                    now
            );
        }
        return new AlertOutcome(created, resolved);
    }

    private OpsVariant decideVariant(OpsExperiment experiment, Long userId, Long orderId) {
        String seed = String.valueOf(userId) + ":" + String.valueOf(orderId);
        int bucket = Math.abs(seed.hashCode()) % 10000;
        BigDecimal ratio = BigDecimal.valueOf(bucket)
                .divide(new BigDecimal("10000"), 4, RoundingMode.DOWN);
        if (ratio.compareTo(experiment.getControlRatio()) < 0) {
            return OpsVariant.CONTROL;
        }
        return OpsVariant.TREATMENT;
    }

    private Map<String, Object> toExperimentPayload(OpsExperiment item) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("experimentId", item.getExperimentId());
        payload.put("experimentKey", item.getExperimentKey());
        payload.put("experimentName", item.getExperimentName());
        payload.put("experimentStatus", item.getExperimentStatus());
        payload.put("controlRatio", item.getControlRatio());
        payload.put("treatmentRatio", item.getTreatmentRatio());
        payload.put("treatmentMarkupDelta", item.getTreatmentMarkupDelta());
        payload.put("treatmentSlaHours", item.getTreatmentSlaHours());
        payload.put("createdBy", item.getCreatedBy());
        payload.put("activatedBy", item.getActivatedBy());
        payload.put("activatedAt", item.getActivatedAt());
        payload.put("createdAt", item.getCreatedAt());
        payload.put("updatedAt", item.getUpdatedAt());
        return payload;
    }

    private Map<String, Object> toAssignmentPayload(OpsExperimentAssignment item) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("assignmentId", item.getAssignmentId());
        payload.put("experimentId", item.getExperimentId());
        payload.put("orderId", item.getOrderId());
        payload.put("userId", item.getUserId());
        payload.put("variant", item.getVariant().name());
        payload.put("baseSlaHours", item.getBaseSlaHours());
        payload.put("finalSlaHours", item.getFinalSlaHours());
        payload.put("baseMarkup", item.getBaseMarkup());
        payload.put("finalMarkup", item.getFinalMarkup());
        payload.put("assignedAt", item.getAssignedAt());
        return payload;
    }

    private Map<String, Object> toAlertPayload(OpsAlertEvent item) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("alertId", item.getAlertId());
        payload.put("alertType", item.getAlertType());
        payload.put("metricKey", item.getMetricKey());
        payload.put("metricValue", item.getMetricValue());
        payload.put("thresholdValue", item.getThresholdValue());
        payload.put("severity", item.getSeverity());
        payload.put("alertStatus", item.getAlertStatus());
        payload.put("detail", item.getDetail());
        payload.put("createdAt", item.getCreatedAt());
        payload.put("resolvedAt", item.getResolvedAt());
        return payload;
    }

    private BigDecimal normalizeRatio(BigDecimal value) {
        if (value == null) {
            throw new ApiException("ratio cannot be null");
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private String normalizeExperimentKey(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ApiException("experimentKey cannot be blank");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int clampSlaHours(Integer value) {
        if (value == null) {
            return 48;
        }
        int raw = value.intValue();
        if (raw < 24) {
            return 24;
        }
        if (raw > 96) {
            return 96;
        }
        return raw;
    }

    private BigDecimal ratio(long numerator, long denominator, int scale, BigDecimal defaultValue) {
        if (denominator <= 0L) {
            return defaultValue.setScale(scale, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), scale, RoundingMode.HALF_UP);
    }

    public static class DispatchOptimizationDecision {

        private final Long experimentId;
        private final OpsVariant variant;
        private final BigDecimal finalMarkup;
        private final Integer finalSlaHours;

        private DispatchOptimizationDecision(Long experimentId,
                                             OpsVariant variant,
                                             BigDecimal finalMarkup,
                                             Integer finalSlaHours) {
            this.experimentId = experimentId;
            this.variant = variant;
            this.finalMarkup = finalMarkup;
            this.finalSlaHours = finalSlaHours;
        }

        public static DispatchOptimizationDecision baseline(BigDecimal finalMarkup, Integer finalSlaHours) {
            return new DispatchOptimizationDecision(null, null, finalMarkup, finalSlaHours);
        }

        public static DispatchOptimizationDecision withExperiment(Long experimentId,
                                                                  OpsVariant variant,
                                                                  BigDecimal finalMarkup,
                                                                  Integer finalSlaHours) {
            return new DispatchOptimizationDecision(experimentId, variant, finalMarkup, finalSlaHours);
        }

        public Long getExperimentId() {
            return experimentId;
        }

        public OpsVariant getVariant() {
            return variant;
        }

        public BigDecimal getFinalMarkup() {
            return finalMarkup;
        }

        public Integer getFinalSlaHours() {
            return finalSlaHours;
        }

        public boolean hasExperiment() {
            return experimentId != null && variant != null;
        }
    }

    private static class AlertOutcome {
        private final int created;
        private final int resolved;

        private AlertOutcome(int created, int resolved) {
            this.created = created;
            this.resolved = resolved;
        }
    }
}
