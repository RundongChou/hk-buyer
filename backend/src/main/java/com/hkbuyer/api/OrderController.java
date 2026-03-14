package com.hkbuyer.api;

import com.hkbuyer.api.dto.CreateOrderRequest;
import com.hkbuyer.api.dto.PayOrderRequest;
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

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    @PostMapping("/{orderId}/pay")
    public Map<String, Object> payOrder(@PathVariable("orderId") Long orderId,
                                        @Valid @RequestBody PayOrderRequest request) {
        return orderService.payOrder(orderId, request.getPaymentChannel());
    }

    @GetMapping("/{orderId}")
    public Map<String, Object> orderDetail(@PathVariable("orderId") Long orderId) {
        return orderService.getOrderDetail(orderId);
    }

    @GetMapping("/{orderId}/timeline")
    public Object orderTimeline(@PathVariable("orderId") Long orderId) {
        return orderService.getTimeline(orderId);
    }
}
