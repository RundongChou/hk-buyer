package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.SubmitBuyerOnboardingRequest;
import com.hkbuyer.domain.BuyerAuditStatus;
import com.hkbuyer.domain.BuyerLevel;
import com.hkbuyer.domain.BuyerOnboardingApplication;
import com.hkbuyer.domain.BuyerProfile;
import com.hkbuyer.repository.BuyerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BuyerService {

    private final BuyerRepository buyerRepository;

    public BuyerService(BuyerRepository buyerRepository) {
        this.buyerRepository = buyerRepository;
    }

    @Transactional
    public Map<String, Object> submitOnboardingApplication(SubmitBuyerOnboardingRequest request) {
        long applicationId = buyerRepository.createOnboardingApplication(
                request.getBuyerId(),
                request.getRealName().trim(),
                normalizeIdCardSuffix(request.getIdCardSuffix()),
                request.getServiceRegion().trim().toUpperCase(Locale.ROOT),
                request.getSpecialtyCategory().trim(),
                request.getSettlementAccount().trim()
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("applicationId", applicationId);
        payload.put("buyerId", request.getBuyerId());
        payload.put("applicationStatus", BuyerAuditStatus.PENDING.name());
        return payload;
    }

    public List<Map<String, Object>> listPendingOnboardingApplications() {
        return buyerRepository.listPendingApplications(200).stream()
                .map(this::toApplicationPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> auditOnboardingApplication(Long applicationId, Long adminId, String decision, String comment) {
        BuyerOnboardingApplication application = buyerRepository.findApplicationById(applicationId)
                .orElseThrow(() -> new ApiException("application not found: " + applicationId));
        if (application.getApplicationStatus() != BuyerAuditStatus.PENDING) {
            throw new ApiException("application has already been audited");
        }

        String normalizedDecision = decision.trim().toUpperCase(Locale.ROOT);
        BuyerAuditStatus targetStatus;
        if ("APPROVE".equals(normalizedDecision) || "APPROVED".equals(normalizedDecision)) {
            targetStatus = BuyerAuditStatus.APPROVED;
        } else if ("REJECT".equals(normalizedDecision) || "REJECTED".equals(normalizedDecision)) {
            targetStatus = BuyerAuditStatus.REJECTED;
        } else {
            throw new ApiException("decision must be APPROVE or REJECT");
        }

        int affected = buyerRepository.auditApplication(applicationId, targetStatus, adminId, comment);
        if (affected == 0) {
            throw new ApiException("application status changed, please retry");
        }

        if (targetStatus == BuyerAuditStatus.APPROVED) {
            buyerRepository.upsertApprovedProfile(application);
        } else {
            buyerRepository.markProfileRejected(application.getBuyerId());
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("applicationId", applicationId);
        payload.put("buyerId", application.getBuyerId());
        payload.put("applicationStatus", targetStatus.name());
        payload.put("adminId", adminId);
        payload.put("comment", comment);
        return payload;
    }

    public Map<String, Object> getBuyerProfile(Long buyerId) {
        BuyerProfile profile = buyerRepository.findProfileByBuyerId(buyerId)
                .orElseThrow(() -> new ApiException("buyer profile not found: " + buyerId));
        return toProfilePayload(profile);
    }

    public BuyerProfile getApprovedProfileOrThrow(Long buyerId) {
        BuyerProfile profile = buyerRepository.findProfileByBuyerId(buyerId)
                .orElseThrow(() -> new ApiException("buyer profile not found: " + buyerId));
        if (profile.getAuditStatus() != BuyerAuditStatus.APPROVED) {
            throw new ApiException("buyer is not approved: " + buyerId);
        }
        return profile;
    }

    public void markTaskAccepted(Long buyerId) {
        buyerRepository.markAcceptedTask(buyerId);
    }

    public void applyProofAuditResult(Long buyerId, boolean approved) {
        buyerRepository.updateReputationAfterProofAudit(buyerId, approved);
    }

    public long countPendingApplications() {
        return buyerRepository.countPendingApplications();
    }

    public long countApprovedProfiles() {
        return buyerRepository.countApprovedProfiles();
    }

    public long countProfilesByLevel(BuyerLevel buyerLevel) {
        return buyerRepository.countProfilesByLevel(buyerLevel);
    }

    private String normalizeIdCardSuffix(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 4) {
            throw new ApiException("idCardSuffix must have at least 4 characters");
        }
        return normalized;
    }

    private Map<String, Object> toApplicationPayload(BuyerOnboardingApplication application) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("applicationId", application.getApplicationId());
        payload.put("buyerId", application.getBuyerId());
        payload.put("realName", application.getRealName());
        payload.put("idCardSuffix", application.getIdCardSuffix());
        payload.put("serviceRegion", application.getServiceRegion());
        payload.put("specialtyCategory", application.getSpecialtyCategory());
        payload.put("settlementAccount", application.getSettlementAccount());
        payload.put("applicationStatus", application.getApplicationStatus().name());
        payload.put("createdAt", application.getCreatedAt());
        return payload;
    }

    private Map<String, Object> toProfilePayload(BuyerProfile profile) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("buyerId", profile.getBuyerId());
        payload.put("displayName", profile.getDisplayName());
        payload.put("auditStatus", profile.getAuditStatus().name());
        payload.put("buyerLevel", profile.getBuyerLevel().name());
        payload.put("creditScore", profile.getCreditScore());
        payload.put("rewardPoints", profile.getRewardPoints());
        payload.put("penaltyPoints", profile.getPenaltyPoints());
        payload.put("acceptedTaskCount", profile.getAcceptedTaskCount());
        payload.put("approvedProofCount", profile.getApprovedProofCount());
        payload.put("rejectedProofCount", profile.getRejectedProofCount());
        payload.put("serviceRegion", profile.getServiceRegion());
        payload.put("specialtyCategory", profile.getSpecialtyCategory());
        payload.put("settlementAccount", profile.getSettlementAccount());
        payload.put("lastActiveAt", profile.getLastActiveAt());
        payload.put("updatedAt", profile.getUpdatedAt());
        return payload;
    }
}
