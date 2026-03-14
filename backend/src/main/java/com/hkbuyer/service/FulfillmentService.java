package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.domain.CustomsClearanceRecord;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.ProcurementTask;
import com.hkbuyer.domain.ShipmentTrackingRecord;
import com.hkbuyer.domain.TaskStatus;
import com.hkbuyer.domain.WarehouseInboundRecord;
import com.hkbuyer.repository.FulfillmentRepository;
import com.hkbuyer.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class FulfillmentService {

    private static final String QC_PASS = "PASS";
    private static final String QC_FAIL = "FAIL";
    private static final String WAREHOUSE_INBOUND_COMPLETED = "INBOUND_COMPLETED";
    private static final String WAREHOUSE_QC_FAILED = "QC_FAILED";
    private static final String CUSTOMS_SUBMITTED = "SUBMITTED";
    private static final String CUSTOMS_RELEASED = "RELEASED";
    private static final String CUSTOMS_REJECTED = "REJECTED";
    private static final String SHIPMENT_IN_TRANSIT = "IN_TRANSIT";
    private static final String SHIPMENT_SIGNED = "SIGNED";
    private static final String DEFAULT_COMPLIANCE_CHANNEL = "GENERAL_TRADE";

    private final FulfillmentRepository fulfillmentRepository;
    private final TaskRepository taskRepository;
    private final OrderService orderService;

    public FulfillmentService(FulfillmentRepository fulfillmentRepository,
                              TaskRepository taskRepository,
                              OrderService orderService) {
        this.fulfillmentRepository = fulfillmentRepository;
        this.taskRepository = taskRepository;
        this.orderService = orderService;
    }

    @Transactional
    public Map<String, Object> submitHandover(Long taskId, Long buyerId, String warehouseCode) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getTaskStatus() != TaskStatus.PROOF_APPROVED) {
            throw new ApiException("task is not ready for handover");
        }
        if (task.getBuyerId() == null || !task.getBuyerId().equals(buyerId)) {
            throw new ApiException("buyer does not match accepted task");
        }

        String normalizedWarehouseCode = normalizeWarehouseCode(warehouseCode);
        LocalDateTime now = LocalDateTime.now();

        fulfillmentRepository.upsertBuyerHandover(task.getOrderId(), taskId, buyerId, normalizedWarehouseCode, now);
        orderService.updateOrderStatus(
                task.getOrderId(),
                OrderStatus.WAIT_INBOUND,
                "warehouse_handover_submitted",
                "买手已交仓，仓库编码: " + normalizedWarehouseCode
        );

        Optional<WarehouseInboundRecord> inbound = fulfillmentRepository.findInboundByOrderId(task.getOrderId());

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("taskId", taskId);
        payload.put("orderId", task.getOrderId());
        payload.put("warehouse", inbound.map(this::toInboundPayload).orElse(null));
        payload.put("orderStatus", OrderStatus.WAIT_INBOUND.name());
        return payload;
    }

    @Transactional
    public Map<String, Object> scanInbound(Long taskId,
                                           String warehouseCode,
                                           String qcDecision,
                                           String qcNote) {
        ProcurementTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("task not found: " + taskId));
        if (task.getTaskStatus() != TaskStatus.PROOF_APPROVED) {
            throw new ApiException("task is not ready for inbound scan");
        }
        if (task.getBuyerId() == null) {
            throw new ApiException("task has no buyer handover owner");
        }

        String normalizedWarehouseCode = normalizeWarehouseCode(warehouseCode);
        String normalizedQcDecision = normalizeQcDecision(qcDecision);
        String warehouseStatus = QC_PASS.equals(normalizedQcDecision)
                ? WAREHOUSE_INBOUND_COMPLETED
                : WAREHOUSE_QC_FAILED;

        LocalDateTime now = LocalDateTime.now();
        if (!fulfillmentRepository.findInboundByOrderId(task.getOrderId()).isPresent()) {
            fulfillmentRepository.upsertBuyerHandover(task.getOrderId(), taskId, task.getBuyerId(), normalizedWarehouseCode, now);
        }

        int affected = fulfillmentRepository.markInboundScan(
                task.getOrderId(),
                normalizedWarehouseCode,
                warehouseStatus,
                normalizedQcDecision,
                trimToNull(qcNote),
                now,
                now
        );
        if (affected == 0) {
            throw new ApiException("inbound record not found for order: " + task.getOrderId());
        }

        String finalNote = trimToNull(qcNote);
        if (QC_PASS.equals(normalizedQcDecision)) {
            orderService.updateOrderStatus(
                    task.getOrderId(),
                    OrderStatus.CUSTOMS_CLEARANCE,
                    "warehouse_qc_passed",
                    "入仓扫描与质检通过，进入清关流程"
            );
        } else {
            orderService.updateOrderStatus(
                    task.getOrderId(),
                    OrderStatus.WAIT_INBOUND,
                    "warehouse_qc_failed",
                    "入仓质检未通过" + (finalNote == null ? "" : "，备注: " + finalNote)
            );
        }

        Optional<WarehouseInboundRecord> inbound = fulfillmentRepository.findInboundByOrderId(task.getOrderId());

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("taskId", taskId);
        payload.put("orderId", task.getOrderId());
        payload.put("warehouse", inbound.map(this::toInboundPayload).orElse(null));
        payload.put("orderStatus", QC_PASS.equals(normalizedQcDecision)
                ? OrderStatus.CUSTOMS_CLEARANCE.name()
                : OrderStatus.WAIT_INBOUND.name());
        return payload;
    }

    @Transactional
    public Map<String, Object> submitCustoms(Long orderId, String declarationNo, String complianceChannel) {
        OrderMain order = orderService.getOrderOrThrow(orderId);
        WarehouseInboundRecord inbound = fulfillmentRepository.findInboundByOrderId(orderId)
                .orElseThrow(() -> new ApiException("inbound record not found for order: " + orderId));
        if (!WAREHOUSE_INBOUND_COMPLETED.equals(inbound.getWarehouseStatus())) {
            throw new ApiException("inbound qc is not completed");
        }

        String normalizedDeclarationNo = normalizeDeclarationNo(declarationNo);
        String normalizedChannel = normalizeComplianceChannel(complianceChannel);
        LocalDateTime now = LocalDateTime.now();

        fulfillmentRepository.upsertCustomsSubmission(order.getOrderId(), normalizedDeclarationNo, normalizedChannel, now);
        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatus.CUSTOMS_CLEARANCE,
                "customs_submitted",
                "清关资料已提交，申报单号: " + normalizedDeclarationNo
        );

        Optional<CustomsClearanceRecord> customs = fulfillmentRepository.findCustomsByOrderId(order.getOrderId());

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getOrderId());
        payload.put("customs", customs.map(this::toCustomsPayload).orElse(null));
        payload.put("orderStatus", OrderStatus.CUSTOMS_CLEARANCE.name());
        return payload;
    }

    @Transactional
    public Map<String, Object> reviewCustoms(Long orderId, String decision, String comment) {
        orderService.getOrderOrThrow(orderId);
        CustomsClearanceRecord customs = fulfillmentRepository.findCustomsByOrderId(orderId)
                .orElseThrow(() -> new ApiException("customs submission not found for order: " + orderId));
        if (!CUSTOMS_SUBMITTED.equals(customs.getClearanceStatus())
                && !CUSTOMS_REJECTED.equals(customs.getClearanceStatus())) {
            throw new ApiException("customs status is not reviewable: " + customs.getClearanceStatus());
        }

        String normalizedDecision = normalizeCustomsDecision(decision);
        LocalDateTime now = LocalDateTime.now();
        String finalComment = trimToNull(comment);

        OrderStatus orderStatus;
        if ("APPROVE".equals(normalizedDecision)) {
            fulfillmentRepository.reviewCustoms(orderId, CUSTOMS_RELEASED, finalComment, now, now);
            orderStatus = OrderStatus.CUSTOMS_CLEARANCE;
            orderService.updateOrderStatus(
                    orderId,
                    orderStatus,
                    "customs_released",
                    "清关放行，等待国内物流揽收"
            );
        } else {
            fulfillmentRepository.reviewCustoms(orderId, CUSTOMS_REJECTED, finalComment, now, null);
            orderStatus = OrderStatus.WAIT_INBOUND;
            orderService.updateOrderStatus(
                    orderId,
                    orderStatus,
                    "customs_rejected",
                    "清关驳回，需补充资料后复核"
            );
        }

        Optional<CustomsClearanceRecord> latestCustoms = fulfillmentRepository.findCustomsByOrderId(orderId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("customs", latestCustoms.map(this::toCustomsPayload).orElse(null));
        payload.put("orderStatus", orderStatus.name());
        payload.put("previousCustomsStatus", customs.getClearanceStatus());
        return payload;
    }

    @Transactional
    public Map<String, Object> updateShipment(Long orderId,
                                              String carrier,
                                              String trackingNo,
                                              String shipmentStatus,
                                              String latestNode) {
        orderService.getOrderOrThrow(orderId);
        CustomsClearanceRecord customs = fulfillmentRepository.findCustomsByOrderId(orderId)
                .orElseThrow(() -> new ApiException("customs submission not found for order: " + orderId));
        if (!CUSTOMS_RELEASED.equals(customs.getClearanceStatus())) {
            throw new ApiException("customs is not released yet");
        }

        String normalizedShipmentStatus = normalizeShipmentStatus(shipmentStatus);
        String normalizedCarrier = normalizeCarrier(carrier);
        String normalizedTrackingNo = normalizeTrackingNo(trackingNo);
        String normalizedLatestNode = normalizeLatestNode(latestNode);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime signedAt = SHIPMENT_SIGNED.equals(normalizedShipmentStatus) ? now : null;

        fulfillmentRepository.upsertShipment(
                orderId,
                normalizedCarrier,
                normalizedTrackingNo,
                normalizedShipmentStatus,
                normalizedLatestNode,
                now,
                signedAt
        );

        if (SHIPMENT_SIGNED.equals(normalizedShipmentStatus)) {
            orderService.updateOrderStatus(
                    orderId,
                    OrderStatus.SIGNED,
                    "shipment_signed",
                    "用户已签收，运单号: " + normalizedTrackingNo
            );
        } else {
            orderService.updateOrderStatus(
                    orderId,
                    OrderStatus.IN_TRANSIT,
                    "shipment_in_transit",
                    "国内物流运输中，承运商: " + normalizedCarrier + "，运单号: " + normalizedTrackingNo
            );
        }

        Optional<ShipmentTrackingRecord> shipment = fulfillmentRepository.findShipmentByOrderId(orderId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("shipment", shipment.map(this::toShipmentPayload).orElse(null));
        payload.put("orderStatus", SHIPMENT_SIGNED.equals(normalizedShipmentStatus)
                ? OrderStatus.SIGNED.name()
                : OrderStatus.IN_TRANSIT.name());
        return payload;
    }

    public Map<String, Object> getOrderFulfillment(Long orderId) {
        OrderMain order = orderService.getOrderOrThrow(orderId);
        Optional<WarehouseInboundRecord> inbound = fulfillmentRepository.findInboundByOrderId(orderId);
        Optional<CustomsClearanceRecord> customs = fulfillmentRepository.findCustomsByOrderId(orderId);
        Optional<ShipmentTrackingRecord> shipment = fulfillmentRepository.findShipmentByOrderId(orderId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getOrderId());
        payload.put("orderStatus", order.getOrderStatus().name());
        payload.put("payStatus", order.getPayStatus());
        payload.put("warehouse", inbound.map(this::toInboundPayload).orElse(null));
        payload.put("customs", customs.map(this::toCustomsPayload).orElse(null));
        payload.put("shipment", shipment.map(this::toShipmentPayload).orElse(null));
        payload.put("timeline", orderService.getTimeline(orderId));
        return payload;
    }

    public long countInboundCompleted() {
        return fulfillmentRepository.countInboundCompleted();
    }

    public long countCustomsSubmitted() {
        return fulfillmentRepository.countCustomsSubmitted();
    }

    public long countCustomsReleased() {
        return fulfillmentRepository.countCustomsReleased();
    }

    public long countCustomsRejected() {
        return fulfillmentRepository.countCustomsRejected();
    }

    public long countShipmentInTransit() {
        return fulfillmentRepository.countShipmentInTransit();
    }

    public long countShipmentSigned() {
        return fulfillmentRepository.countShipmentSigned();
    }

    public long countSignedWithinSevenToFifteenDays() {
        return fulfillmentRepository.countSignedWithinSevenToFifteenDays();
    }

    private String normalizeWarehouseCode(String warehouseCode) {
        String value = trimToNull(warehouseCode);
        if (value == null) {
            throw new ApiException("warehouseCode must not be blank");
        }
        return value.toUpperCase();
    }

    private String normalizeDeclarationNo(String declarationNo) {
        String value = trimToNull(declarationNo);
        if (value == null) {
            throw new ApiException("declarationNo must not be blank");
        }
        return value.toUpperCase();
    }

    private String normalizeComplianceChannel(String complianceChannel) {
        String value = trimToNull(complianceChannel);
        if (value == null) {
            return DEFAULT_COMPLIANCE_CHANNEL;
        }
        return value.toUpperCase();
    }

    private String normalizeQcDecision(String qcDecision) {
        String value = trimToNull(qcDecision);
        if (value == null) {
            throw new ApiException("qcDecision must be PASS or FAIL");
        }
        String normalized = value.toUpperCase();
        if (QC_PASS.equals(normalized) || QC_FAIL.equals(normalized)) {
            return normalized;
        }
        throw new ApiException("qcDecision must be PASS or FAIL");
    }

    private String normalizeCustomsDecision(String decision) {
        String value = trimToNull(decision);
        if (value == null) {
            throw new ApiException("decision must be APPROVE or REJECT");
        }
        String normalized = value.toUpperCase();
        if ("APPROVE".equals(normalized) || "REJECT".equals(normalized)) {
            return normalized;
        }
        throw new ApiException("decision must be APPROVE or REJECT");
    }

    private String normalizeShipmentStatus(String shipmentStatus) {
        String value = trimToNull(shipmentStatus);
        if (value == null) {
            throw new ApiException("shipmentStatus must be IN_TRANSIT or SIGNED");
        }
        String normalized = value.toUpperCase();
        if (SHIPMENT_IN_TRANSIT.equals(normalized) || SHIPMENT_SIGNED.equals(normalized)) {
            return normalized;
        }
        throw new ApiException("shipmentStatus must be IN_TRANSIT or SIGNED");
    }

    private String normalizeCarrier(String carrier) {
        String value = trimToNull(carrier);
        if (value == null) {
            throw new ApiException("carrier must not be blank");
        }
        return value;
    }

    private String normalizeTrackingNo(String trackingNo) {
        String value = trimToNull(trackingNo);
        if (value == null) {
            throw new ApiException("trackingNo must not be blank");
        }
        return value.toUpperCase();
    }

    private String normalizeLatestNode(String latestNode) {
        String value = trimToNull(latestNode);
        if (value == null) {
            throw new ApiException("latestNode must not be blank");
        }
        return value;
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

    private Map<String, Object> toInboundPayload(WarehouseInboundRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("inboundId", record.getInboundId());
        payload.put("orderId", record.getOrderId());
        payload.put("taskId", record.getTaskId());
        payload.put("buyerId", record.getBuyerId());
        payload.put("warehouseCode", record.getWarehouseCode());
        payload.put("handoverAt", record.getHandoverAt());
        payload.put("warehouseStatus", record.getWarehouseStatus());
        payload.put("qcStatus", record.getQcStatus());
        payload.put("qcNote", record.getQcNote());
        payload.put("scannedAt", record.getScannedAt());
        payload.put("inspectedAt", record.getInspectedAt());
        payload.put("updatedAt", record.getUpdatedAt());
        return payload;
    }

    private Map<String, Object> toCustomsPayload(CustomsClearanceRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("clearanceId", record.getClearanceId());
        payload.put("orderId", record.getOrderId());
        payload.put("declarationNo", record.getDeclarationNo());
        payload.put("clearanceStatus", record.getClearanceStatus());
        payload.put("complianceChannel", record.getComplianceChannel());
        payload.put("reviewComment", record.getReviewComment());
        payload.put("submittedAt", record.getSubmittedAt());
        payload.put("reviewedAt", record.getReviewedAt());
        payload.put("releasedAt", record.getReleasedAt());
        payload.put("updatedAt", record.getUpdatedAt());
        return payload;
    }

    private Map<String, Object> toShipmentPayload(ShipmentTrackingRecord record) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("shipmentId", record.getShipmentId());
        payload.put("orderId", record.getOrderId());
        payload.put("carrier", record.getCarrier());
        payload.put("trackingNo", record.getTrackingNo());
        payload.put("shipmentStatus", record.getShipmentStatus());
        payload.put("latestNode", record.getLatestNode());
        payload.put("latestNodeAt", record.getLatestNodeAt());
        payload.put("signedAt", record.getSignedAt());
        payload.put("updatedAt", record.getUpdatedAt());
        return payload;
    }
}
