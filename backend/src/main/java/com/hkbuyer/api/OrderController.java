package com.hkbuyer.api;

import com.hkbuyer.api.dto.CreateOrderRequest;
import com.hkbuyer.api.dto.CreateAuthenticityDisputeRequest;
import com.hkbuyer.api.dto.CompensatePayRequest;
import com.hkbuyer.api.dto.PayOrderRequest;
import com.hkbuyer.api.dto.SubmitAfterSaleDecisionRequest;
import com.hkbuyer.service.AfterSaleService;
import com.hkbuyer.service.FulfillmentService;
import com.hkbuyer.service.OrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final FulfillmentService fulfillmentService;
    private final AfterSaleService afterSaleService;

    public OrderController(OrderService orderService,
                           FulfillmentService fulfillmentService,
                           AfterSaleService afterSaleService) {
        this.orderService = orderService;
        this.fulfillmentService = fulfillmentService;
        this.afterSaleService = afterSaleService;
    }

    @PostMapping
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{orderId}/pay")
    public Map<String, Object> payOrder(@PathVariable("orderId") Long orderId,
                                        @Valid @RequestBody PayOrderRequest request) {
        return orderService.payOrder(orderId, request.getPaymentChannel(), request.getPaymentScenario());
    }

    @PostMapping("/{orderId}/pay-compensate")
    public Map<String, Object> compensatePay(@PathVariable("orderId") Long orderId,
                                             @Valid @RequestBody CompensatePayRequest request) {
        return orderService.compensatePay(orderId, request.getPaymentChannel(), request.getCompensationToken());
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> orderDetail(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

    @GetMapping("/{orderId}/timeline")
    public Object orderTimeline(@PathVariable("orderId") Long orderId) {
        return orderService.getTimeline(orderId);
    }

    @GetMapping("/{orderId}/fulfillment")
    public Map<String, Object> orderFulfillment(@PathVariable("orderId") Long orderId) {
        return fulfillmentService.getOrderFulfillment(orderId);
    }

    @PostMapping("/{orderId}/after-sale/authenticity")
    public Map<String, Object> createAuthenticityDispute(@PathVariable("orderId") Long orderId,
                                                         @Valid @RequestBody CreateAuthenticityDisputeRequest request) {
        return afterSaleService.createAuthenticityDispute(
                orderId,
                request.getUserId(),
                request.getIssueReason(),
                request.getEvidenceUrl()
        );
    }

    @PostMapping("/{orderId}/after-sale/cases/{caseId}/decision")
    public Map<String, Object> submitAfterSaleDecision(@PathVariable("orderId") Long orderId,
                                                       @PathVariable("caseId") Long caseId,
                                                       @Valid @RequestBody SubmitAfterSaleDecisionRequest request) {
        return afterSaleService.submitUserDecision(
                orderId,
                caseId,
                request.getUserId(),
                request.getDecision(),
                request.getComment()
        );
    }

    @GetMapping("/{orderId}/after-sale/cases")
    public Object orderAfterSaleCases(@PathVariable("orderId") Long orderId,
                                      @RequestParam(value = "userId", required = false) Long userId) {
        return afterSaleService.listOrderCases(orderId, userId);
    }
}
