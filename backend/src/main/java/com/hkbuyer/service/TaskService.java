package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.SubmitProofRequest;
import com.hkbuyer.domain.BuyerProfile;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.repository.ProofRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private static final BigDecimal AUTO_MARKUP_RATE = new BigDecimal("0.05");
    private static final BigDecimal AUTO_MARKUP_MIN_INCREMENT = new BigDecimal("20.00");
    private static final BigDecimal AUTO_MARKUP_CAP_RATE = new BigDecimal("0.20");
    private static final int AUTO_MARKUP_MAX_COUNT = 3;
    private static final int AUTO_MARKUP_COOLDOWN_HOURS = 24;
    private static final int REDISPATCH_ACCEPT_WINDOW_HOURS = 72;
    private static final int AUTO_REPRICE_BATCH_LIMIT = 200;

    private final TaskRepository taskRepository;
    private final ProofRepository proofRepository;
    private final OrderService orderService;
    private final BuyerService buyerService;

    public TaskService(TaskRepository taskRepository,
                       ProofRepository proofRepository,
                       OrderService orderService,
                       BuyerService buyerService) {
        this.taskRepository = taskRepository;
        this.proofRepository = proofRepository;
        this.orderService = orderService;
        this.buyerService = buyerService;
    }

    public List<Map<String, Object>> listPublishedTasks(Long buyerId) {
        List<ProcurementTask> tasks = taskRepository.listPublishedTasks();
        if (buyerId == null) {
            return tasks.stream().map(this::toTaskPayload).collect(Collectors.toList());
        }

        BuyerProfile profile = buyerService.getApprovedProfileOrThrow(buyerId);
        return tasks.stream()
                .filter(task -> isTaskEligible(task, profile))
                .map(this::toTaskPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> acceptTask(Long taskId, Long buyerId) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getTaskStatus() != TaskStatus.PUBLISHED) {
            throw new ApiException("task is not in PUBLISHED status");
        }

        BuyerProfile profile = buyerService.getApprovedProfileOrThrow(buyerId);
        if (!isTaskEligible(task, profile)) {
            throw new ApiException("buyer is not eligible for this task");
        }

        int affected = taskRepository.acceptTask(taskId, buyerId);
        if (affected == 0) {
            throw new ApiException("task has been accepted by another buyer");
        }
        buyerService.markTaskAccepted(buyerId);
        orderService.updateOrderStatus(task.getOrderId(), OrderStatus.BUYER_PROCUREMENT, "task_accepted", "买手已接单，买手ID: " + buyerId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("taskId", taskId);
        payload.put("taskStatus", TaskStatus.ACCEPTED.name());
        payload.put("buyerId", buyerId);
        payload.put("taskTier", task.getTaskTier().name());
        payload.put("requiredBuyerLevel", task.getRequiredBuyerLevel().name());
        return payload;
    }

    @Transactional
    public Map<String, Object> submitProof(Long taskId, SubmitProofRequest request) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getTaskStatus() != TaskStatus.ACCEPTED && task.getTaskStatus() != TaskStatus.PROOF_REJECTED) {
            throw new ApiException("task is not ready for proof submission");
        }
        if (task.getBuyerId() != null && !task.getBuyerId().equals(request.getBuyerId())) {
            throw new ApiException("buyer does not match accepted task");
        }

        LocalDate expiryDate;
        try {
            expiryDate = LocalDate.parse(request.getExpiryDate());
        } catch (DateTimeParseException ex) {
            throw new ApiException("expiryDate must use ISO format yyyy-MM-dd");
        }

        long proofId = proofRepository.createProof(
                taskId,
                request.getBuyerId(),
                request.getStoreName(),
                request.getReceiptUrl(),
                request.getBatchNo(),
                expiryDate,
                request.getProductPhotoUrl()
        );

        taskRepository.markProofSubmitted(taskId);
        orderService.updateOrderStatus(task.getOrderId(), OrderStatus.PROOF_UNDER_REVIEW, "proof_submitted", "买手已提交采购凭证");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("proofId", proofId);
        payload.put("taskId", taskId);
        payload.put("taskStatus", TaskStatus.PROOF_SUBMITTED.name());
        return payload;
    }

    public long countAcceptedTasks() {
        return taskRepository.countAcceptedTasks();
    }

    public long countTimeoutUnacceptedTasks() {
        return taskRepository.countTimeoutUnacceptedTasks();
    }

    public List<Map<String, Object>> listTimeoutCandidates() {
        List<ProcurementTask> tasks = taskRepository.listTimeoutCandidates(AUTO_REPRICE_BATCH_LIMIT);
        LocalDateTime now = LocalDateTime.now();
        return tasks.stream()
                .map(task -> toTimeoutCandidatePayload(task, now))
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> runTimeoutReprice() {
        List<ProcurementTask> candidates = taskRepository.listTimeoutCandidates(AUTO_REPRICE_BATCH_LIMIT);
        LocalDateTime now = LocalDateTime.now();
        int repricedCount = 0;
        int skippedFrequencyCount = 0;
        int terminatedCount = 0;
        int concurrencySkippedCount = 0;
        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();

        for (ProcurementTask task : candidates) {
            BigDecimal orderTotal = resolveOrderTotal(task.getOrderId());
            BigDecimal currentMarkup = normalizeMoney(task.getSuggestedMarkup());
            BigDecimal markupCap = calculateMarkupCap(orderTotal);

            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("taskId", task.getTaskId());
            detail.put("orderId", task.getOrderId());
            detail.put("orderTotalAmount", orderTotal);
            detail.put("currentSuggestedMarkup", currentMarkup);
            detail.put("markupCap", markupCap);
            detail.put("markupAppliedCount", normalizeCount(task.getMarkupAppliedCount()));
            detail.put("redispatchCount", normalizeCount(task.getRedispatchCount()));

            if (shouldTerminate(task, currentMarkup, markupCap)) {
                int affected = taskRepository.markTimeoutTaskExpired(task.getTaskId(), "MAX_REPRICE_LIMIT_REACHED");
                if (affected == 0) {
                    concurrencySkippedCount++;
                    detail.put("action", "SKIPPED_CONCURRENT_UPDATE");
                } else {
                    terminatedCount++;
                    orderService.updateOrderStatus(
                            task.getOrderId(),
                            OrderStatus.CANCELLED,
                            "task_auto_terminated",
                            "任务超时且达到提价上限，系统自动终止"
                    );
                    detail.put("action", "TERMINATED");
                    detail.put("terminalReason", "MAX_REPRICE_LIMIT_REACHED");
                }
                details.add(detail);
                continue;
            }

            if (isFrequencyLimited(task, now)) {
                skippedFrequencyCount++;
                detail.put("action", "SKIPPED_FREQUENCY_LIMIT");
                detail.put("nextMarkupEligibleAt", task.getNextMarkupEligibleAt());
                details.add(detail);
                continue;
            }

            BigDecimal markupIncrement = calculateMarkupIncrement(orderTotal);
            BigDecimal newSuggestedMarkup = currentMarkup.add(markupIncrement);
            if (newSuggestedMarkup.compareTo(markupCap) > 0) {
                newSuggestedMarkup = markupCap;
            }
            newSuggestedMarkup = normalizeMoney(newSuggestedMarkup);

            if (newSuggestedMarkup.compareTo(currentMarkup) <= 0) {
                int affected = taskRepository.markTimeoutTaskExpired(task.getTaskId(), "MARKUP_CAP_REACHED");
                if (affected == 0) {
                    concurrencySkippedCount++;
                    detail.put("action", "SKIPPED_CONCURRENT_UPDATE");
                } else {
                    terminatedCount++;
                    orderService.updateOrderStatus(
                            task.getOrderId(),
                            OrderStatus.CANCELLED,
                            "task_auto_terminated",
                            "任务超时且达到提价封顶，系统自动终止"
                    );
                    detail.put("action", "TERMINATED");
                    detail.put("terminalReason", "MARKUP_CAP_REACHED");
                }
                details.add(detail);
                continue;
            }

            LocalDateTime nextMarkupEligibleAt = now.plusHours(AUTO_MARKUP_COOLDOWN_HOURS);
            LocalDateTime newAcceptDeadline = now.plusHours(REDISPATCH_ACCEPT_WINDOW_HOURS);
            int affected = taskRepository.applyTimeoutMarkupAndRedispatch(
                    task.getTaskId(),
                    newSuggestedMarkup,
                    nextMarkupEligibleAt,
                    newAcceptDeadline
            );
            if (affected == 0) {
                concurrencySkippedCount++;
                detail.put("action", "SKIPPED_CONCURRENT_UPDATE");
                details.add(detail);
                continue;
            }

            repricedCount++;
            orderService.updateOrderStatus(
                    task.getOrderId(),
                    OrderStatus.PAID_WAIT_ACCEPT,
                    "task_auto_repriced",
                    "72h无人接单，系统自动提价重派，建议加价调整为: " + newSuggestedMarkup.toPlainString()
            );
            detail.put("action", "REPRICED_AND_REDISPATCHED");
            detail.put("newSuggestedMarkup", newSuggestedMarkup);
            detail.put("nextMarkupEligibleAt", nextMarkupEligibleAt);
            detail.put("newAcceptDeadline", newAcceptDeadline);
            details.add(detail);
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("runAt", now);
        payload.put("batchSize", Integer.valueOf(candidates.size()));
        payload.put("repricedCount", Integer.valueOf(repricedCount));
        payload.put("skippedFrequencyCount", Integer.valueOf(skippedFrequencyCount));
        payload.put("terminatedCount", Integer.valueOf(terminatedCount));
        payload.put("concurrencySkippedCount", Integer.valueOf(concurrencySkippedCount));
        payload.put("details", details);
        return payload;
    }

    public long countTimeoutCandidates() {
        return taskRepository.countTimeoutCandidates();
    }

    public long countFrequencyLimitedTimeoutCandidates() {
        return taskRepository.countFrequencyLimitedTimeoutCandidates();
    }

    public long countTasksWithAutoMarkup() {
        return taskRepository.countTasksWithAutoMarkup();
    }

    public long sumAutoMarkupAppliedCount() {
        return taskRepository.sumAutoMarkupAppliedCount();
    }

    public long sumRedispatchCount() {
        return taskRepository.sumRedispatchCount();
    }

    public long countTimeoutTerminatedTasks() {
        return taskRepository.countTimeoutTerminatedTasks();
    }

    public long countAcceptedAfterAutoMarkup() {
        return taskRepository.countAcceptedAfterAutoMarkup();
    }

    private Map<String, Object> toTaskPayload(ProcurementTask task) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("taskId", task.getTaskId());
        payload.put("orderId", task.getOrderId());
        payload.put("taskStatus", task.getTaskStatus().name());
        payload.put("acceptDeadline", task.getAcceptDeadline());
        payload.put("suggestedMarkup", task.getSuggestedMarkup());
        payload.put("taskTier", task.getTaskTier().name());
        payload.put("requiredBuyerLevel", task.getRequiredBuyerLevel().name());
        payload.put("targetRegion", task.getTargetRegion());
        payload.put("targetCategory", task.getTargetCategory());
        payload.put("slaHours", task.getSlaHours());
        payload.put("markupAppliedCount", task.getMarkupAppliedCount());
        payload.put("redispatchCount", task.getRedispatchCount());
        payload.put("lastMarkupAt", task.getLastMarkupAt());
        payload.put("nextMarkupEligibleAt", task.getNextMarkupEligibleAt());
        payload.put("terminalReason", task.getTerminalReason());
        return payload;
    }

    private Map<String, Object> toTimeoutCandidatePayload(ProcurementTask task, LocalDateTime now) {
        BigDecimal orderTotal = resolveOrderTotal(task.getOrderId());
        BigDecimal currentMarkup = normalizeMoney(task.getSuggestedMarkup());
        BigDecimal markupCap = calculateMarkupCap(orderTotal);
        boolean frequencyLimited = isFrequencyLimited(task, now);
        boolean alreadyAtCapOrLimit = shouldTerminate(task, currentMarkup, markupCap);
        Map<String, Object> payload = toTaskPayload(task);
        payload.put("orderTotalAmount", orderTotal);
        payload.put("markupCap", markupCap);
        payload.put("canAutoReprice", Boolean.valueOf(!frequencyLimited && !alreadyAtCapOrLimit));
        payload.put("frequencyLimited", Boolean.valueOf(frequencyLimited));
        payload.put("alreadyAtCapOrLimit", Boolean.valueOf(alreadyAtCapOrLimit));
        return payload;
    }

    private BigDecimal resolveOrderTotal(Long orderId) {
        if (orderId == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalAmount = orderService.getOrderOrThrow(orderId).getTotalAmount();
        return normalizeMoney(totalAmount);
    }

    private boolean isFrequencyLimited(ProcurementTask task, LocalDateTime now) {
        LocalDateTime nextMarkupEligibleAt = task.getNextMarkupEligibleAt();
        return nextMarkupEligibleAt != null && nextMarkupEligibleAt.isAfter(now);
    }

    private boolean shouldTerminate(ProcurementTask task, BigDecimal currentMarkup, BigDecimal markupCap) {
        if (normalizeCount(task.getMarkupAppliedCount()) >= AUTO_MARKUP_MAX_COUNT) {
            return true;
        }
        if (markupCap.signum() <= 0) {
            return true;
        }
        return currentMarkup.compareTo(markupCap) >= 0;
    }

    private BigDecimal calculateMarkupIncrement(BigDecimal orderTotal) {
        BigDecimal byRate = orderTotal.multiply(AUTO_MARKUP_RATE);
        if (byRate.compareTo(AUTO_MARKUP_MIN_INCREMENT) < 0) {
            return AUTO_MARKUP_MIN_INCREMENT;
        }
        return normalizeMoney(byRate);
    }

    private BigDecimal calculateMarkupCap(BigDecimal orderTotal) {
        return normalizeMoney(orderTotal.multiply(AUTO_MARKUP_CAP_RATE));
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private int normalizeCount(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private boolean isTaskEligible(ProcurementTask task, BuyerProfile profile) {
        if (!profile.getBuyerLevel().isAtLeast(task.getRequiredBuyerLevel())) {
            return false;
        }
        if (!matchesRegion(task.getTargetRegion(), profile.getServiceRegion())) {
            return false;
        }
        return matchesCategory(task.getTargetCategory(), profile.getSpecialtyCategory());
    }

    private boolean matchesRegion(String taskRegion, String buyerRegion) {
        if (taskRegion == null || taskRegion.trim().isEmpty() || "ALL".equalsIgnoreCase(taskRegion)) {
            return true;
        }
        if (buyerRegion == null || buyerRegion.trim().isEmpty()) {
            return false;
        }
        return taskRegion.equalsIgnoreCase(buyerRegion);
    }

    private boolean matchesCategory(String taskCategory, String buyerCategory) {
        if (taskCategory == null
                || taskCategory.trim().isEmpty()
                || "GENERAL".equalsIgnoreCase(taskCategory)
                || "ALL".equalsIgnoreCase(taskCategory)) {
            return true;
        }
        if (buyerCategory == null || buyerCategory.trim().isEmpty()) {
            return false;
        }
        return taskCategory.equalsIgnoreCase(buyerCategory);
    }
}
