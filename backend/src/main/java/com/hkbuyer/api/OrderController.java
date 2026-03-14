package com.hkbuyer.api;

import com.hkbuyer.api.dto.CreateOrderRequest;
import com.hkbuyer.api.dto.CompensatePayRequest;
import com.hkbuyer.api.dto.PayOrderRequest;
import com.hkbuyer.service.FulfillmentService;
import com.hkbuyer.service.OrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final FulfillmentService fulfillmentService;

    public OrderController(OrderService orderService,
                           FulfillmentService fulfillmentService) {
        this.orderService = orderService;
        this.fulfillmentService = fulfillmentService;
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
}
