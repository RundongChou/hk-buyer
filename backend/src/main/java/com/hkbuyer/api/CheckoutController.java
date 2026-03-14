package com.hkbuyer.api;

import com.hkbuyer.api.dto.CheckoutQuoteRequest;
import com.hkbuyer.api.dto.CreateOrderFromCartRequest;
import com.hkbuyer.service.OrderService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/quote")
    public Map<String, Object> quote(@Valid @RequestBody CheckoutQuoteRequest request) {
        return orderService.quoteCart(request.getUserId(), request.getCouponCode());
    }

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@Valid @RequestBody CreateOrderFromCartRequest request) {
        return orderService.createOrderFromCart(request.getUserId(), request.getCouponCode());
    }
}
