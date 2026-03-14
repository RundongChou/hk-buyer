package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProofAuditStatus;
import com.hkbuyer.domain.PurchaseProof;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.repository.ProofRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProofService {

    private final ProofRepository proofRepository;
    private final TaskRepository taskRepository;
    private final OrderService orderService;

    public ProofService(ProofRepository proofRepository,
                        TaskRepository taskRepository,
                        OrderService orderService) {
        this.proofRepository = proofRepository;
        this.taskRepository = taskRepository;
        this.orderService = orderService;
    }

    public List<Map<String, Object>> listPendingProofs() {
        return proofRepository.listPendingProofs().stream().map(proof -> {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("proofId", proof.getProofId());
            payload.put("taskId", proof.getTaskId());
            payload.put("buyerId", proof.getBuyerId());
            payload.put("storeName", proof.getStoreName());
            payload.put("receiptUrl", proof.getReceiptUrl());
            payload.put("batchNo", proof.getBatchNo());
            payload.put("expiryDate", proof.getExpiryDate());
            payload.put("productPhotoUrl", proof.getProductPhotoUrl());
            payload.put("auditStatus", proof.getAuditStatus().name());
            payload.put("createdAt", proof.getCreatedAt());
            return payload;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> auditProof(Long proofId, Long adminId, String decision, String comment) {
        PurchaseProof proof = proofRepository.findById(proofId)
                .orElseThrow(() -> new ApiException("proof not found: " + proofId));
        if (proof.getAuditStatus() != ProofAuditStatus.PENDING) {
            throw new ApiException("proof has already been audited");
        }

        String normalized = decision.toUpperCase(Locale.ROOT);
        ProofAuditStatus auditStatus;
        TaskStatus taskStatus;
        OrderStatus orderStatus;
        String eventType;
        String eventDescription;

        if ("APPROVE".equals(normalized) || "APPROVED".equals(normalized)) {
            auditStatus = ProofAuditStatus.APPROVED;
            taskStatus = TaskStatus.PROOF_APPROVED;
            orderStatus = OrderStatus.WAIT_INBOUND;
            eventType = "proof_approved";
            eventDescription = "后台审核通过，待入仓";
        } else if ("REJECT".equals(normalized) || "REJECTED".equals(normalized)) {
            auditStatus = ProofAuditStatus.REJECTED;
            taskStatus = TaskStatus.PROOF_REJECTED;
            orderStatus = OrderStatus.BUYER_PROCUREMENT;
            eventType = "proof_rejected";
            eventDescription = "后台驳回凭证，等待买手补传";
        } else {
            throw new ApiException("decision must be APPROVE or REJECT");
        }

        proofRepository.auditProof(proofId, auditStatus, adminId, comment);
        taskRepository.markAuditResult(proof.getTaskId(), taskStatus);

        Long orderId = taskRepository.findOrderIdByTaskId(proof.getTaskId());
        orderService.updateOrderStatus(orderId, orderStatus, eventType, eventDescription);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("proofId", proofId);
        payload.put("auditStatus", auditStatus.name());
        payload.put("orderId", orderId);
        payload.put("orderStatus", orderStatus.name());
        payload.put("taskStatus", taskStatus.name());
        return payload;
    }

    public long countSubmittedProofs() {
        return proofRepository.countSubmittedProofs();
    }
}
