package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.SubmitProofRequest;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.repository.ProofRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProofRepository proofRepository;
    private final OrderService orderService;

    public TaskService(TaskRepository taskRepository,
                       ProofRepository proofRepository,
                       OrderService orderService) {
        this.taskRepository = taskRepository;
        this.proofRepository = proofRepository;
        this.orderService = orderService;
    }

    public List<Map<String, Object>> listPublishedTasks() {
        return taskRepository.listPublishedTasks().stream().map(task -> {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("taskId", task.getTaskId());
            payload.put("orderId", task.getOrderId());
            payload.put("taskStatus", task.getTaskStatus().name());
            payload.put("acceptDeadline", task.getAcceptDeadline());
            payload.put("suggestedMarkup", task.getSuggestedMarkup());
            return payload;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> acceptTask(Long taskId, Long buyerId) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getTaskStatus() != TaskStatus.PUBLISHED) {
            throw new ApiException("task is not in PUBLISHED status");
        }

        taskRepository.acceptTask(taskId, buyerId);
        orderService.updateOrderStatus(task.getOrderId(), OrderStatus.BUYER_PROCUREMENT, "task_accepted", "买手已接单，买手ID: " + buyerId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("taskId", taskId);
        payload.put("taskStatus", TaskStatus.ACCEPTED.name());
        payload.put("buyerId", buyerId);
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
}
