package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.CreateOrderItemRequest;
import com.hkbuyer.api.dto.CreateOrderRequest;
import com.hkbuyer.domain.OrderItem;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.TimelineEvent;
import com.hkbuyer.repository.OrderRepository;
import com.hkbuyer.repository.TaskRepository;
import com.hkbuyer.repository.TimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final TimelineRepository timelineRepository;

    public OrderService(OrderRepository orderRepository,
                        TaskRepository taskRepository,
                        TimelineRepository timelineRepository) {
        this.orderRepository = orderRepository;
        this.taskRepository = taskRepository;
        this.timelineRepository = timelineRepository;
    }

    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderItemRequest item : request.getItems()) {
            if (item.getUnitPrice().signum() <= 0) {
                throw new ApiException("unitPrice must be positive");
            }
            BigDecimal lineAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty().longValue()));
            totalAmount = totalAmount.add(lineAmount);
        }

        long orderId = orderRepository.createOrder(request.getUserId(), totalAmount);
        for (CreateOrderItemRequest item : request.getItems()) {
            BigDecimal lineAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty().longValue()));
            orderRepository.addItem(orderId, item.getSkuId(), item.getQty(), item.getUnitPrice(), lineAmount);
        }
        timelineRepository.addEvent(orderId, "order_submitted", "订单已提交，待支付");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("orderStatus", OrderStatus.PENDING_PAYMENT.name());
        payload.put("totalAmount", totalAmount);
        return payload;
    }

    @Transactional
    public Map<String, Object> payOrder(Long orderId, String paymentChannel) {
        OrderMain order = getOrderOrThrow(orderId);
        if ("PAID".equals(order.getPayStatus())) {
            throw new ApiException("order already paid");
        }

        orderRepository.markPaid(orderId);
        timelineRepository.addEvent(orderId, "payment_success", "支付成功，渠道: " + paymentChannel);

        long taskId = taskRepository.createTask(
                orderId,
                new BigDecimal("20.00"),
                LocalDateTime.now().plusHours(72)
        );
        timelineRepository.addEvent(orderId, "task_published", "采购任务已发布");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("taskId", taskId);
        payload.put("orderStatus", OrderStatus.PAID_WAIT_ACCEPT.name());
        payload.put("payStatus", "PAID");
        return payload;
    }

    public Map<String, Object> getOrderDetail(Long orderId) {
        OrderMain order = getOrderOrThrow(orderId);
        List<OrderItem> items = orderRepository.findItems(orderId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", order.getOrderId());
        payload.put("userId", order.getUserId());
        payload.put("orderStatus", order.getOrderStatus().name());
        payload.put("payStatus", order.getPayStatus());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("createdAt", order.getCreatedAt());
        payload.put("updatedAt", order.getUpdatedAt());
        payload.put("items", items);
        return payload;
    }

    public List<TimelineEvent> getTimeline(Long orderId) {
        getOrderOrThrow(orderId);
        return timelineRepository.listByOrderId(orderId);
    }

    public OrderMain getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("order not found: " + orderId));
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus orderStatus, String eventType, String eventDescription) {
        orderRepository.updateStatus(orderId, orderStatus);
        timelineRepository.addEvent(orderId, eventType, eventDescription);
    }

    public long countPaidOrders() {
        return orderRepository.countPaidOrders();
    }
}
