package com.hkbuyer.api;

import com.hkbuyer.api.dto.AuditProofRequest;
import com.hkbuyer.service.MetricsService;
import com.hkbuyer.service.ProofService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ProofService proofService;
    private final MetricsService metricsService;

    public AdminController(ProofService proofService, MetricsService metricsService) {
        this.proofService = proofService;
        this.metricsService = metricsService;
    }

    @GetMapping("/proofs/pending")
    public List<Map<String, Object>> listPendingProofs() {
        return proofService.listPendingProofs();
    }

    @PostMapping("/proofs/{proofId}/audit")
    public Map<String, Object> auditProof(@PathVariable("proofId") Long proofId,
                                          @Valid @RequestBody AuditProofRequest request) {
        return proofService.auditProof(proofId, request.getAdminId(), request.getDecision(), request.getComment());
    }

    @GetMapping("/metrics/funnel")
    public Map<String, Object> funnelMetrics() {
        return metricsService.buildFunnelMetrics();
    }
}
