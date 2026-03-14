package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.CreateOrderItemRequest;
import com.hkbuyer.api.dto.CreateOrderRequest;
import com.hkbuyer.domain.CartItem;
import com.hkbuyer.domain.BuyerLevel;
import com.hkbuyer.domain.CouponTemplate;
import com.hkbuyer.domain.OrderItem;
import com.hkbuyer.domain.OrderMain;
import com.hkbuyer.domain.OrderStatus;
import com.hkbuyer.domain.TaskTier;
import com.hkbuyer.domain.TimelineEvent;
import com.hkbuyer.repository.CartRepository;
import com.hkbuyer.repository.CouponRepository;
import com.hkbuyer.repository.OrderRepository;
import com.hkbuyer.repository.PaymentCompensationRepository;
import com.hkbuyer.repository.TaskRepository;
import com.hkbuyer.repository.TimelineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private static final BigDecimal TAX_ESTIMATE_RATE = new BigDecimal("0.09");

    private final OrderRepository orderRepository;
    private final TaskRepository taskRepository;
    private final TimelineRepository timelineRepository;
    private final CatalogService catalogService;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;
    private final PaymentCompensationRepository paymentCompensationRepository;

    public OrderService(OrderRepository orderRepository,
                        TaskRepository taskRepository,
                        TimelineRepository timelineRepository,
                        CatalogService catalogService,
                        CartRepository cartRepository,
                        CouponRepository couponRepository,
                        PaymentCompensationRepository paymentCompensationRepository) {
        this.orderRepository = orderRepository;
        this.taskRepository = taskRepository;
        this.timelineRepository = timelineRepository;
        this.catalogService = catalogService;
        this.cartRepository = cartRepository;
        this.couponRepository = couponRepository;
        this.paymentCompensationRepository = paymentCompensationRepository;
    }

    @Transactional
    public Map<String, Object> createOrder(CreateOrderRequest request) {
        List<CatalogService.ResolvedOrderLine> resolvedLines = new ArrayList<CatalogService.ResolvedOrderLine>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderItemRequest item : request.getItems()) {
            CatalogService.ResolvedOrderLine resolvedLine = catalogService.resolveOrderLine(
                    item.getSkuId(),
                    item.getQty(),
                    item.getUnitPrice()
            );
            resolvedLines.add(resolvedLine);
            BigDecimal lineAmount = resolvedLine.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty().longValue()));
            totalAmount = totalAmount.add(lineAmount);
        }

        long orderId = orderRepository.createOrder(request.getUserId(), totalAmount);
        for (CatalogService.ResolvedOrderLine line : resolvedLines) {
            BigDecimal lineAmount = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty().longValue()));
            orderRepository.addItem(orderId, line.getSkuId(), line.getQty(), line.getUnitPrice(), lineAmount);
            catalogService.consumeStockForOrder(line, Long.valueOf(orderId));
        }
        timelineRepository.addEvent(orderId, "order_submitted", "订单已提交，待支付");

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("orderStatus", OrderStatus.PENDING_PAYMENT.name());
        payload.put("totalAmount", totalAmount);
        return payload;
    }

    public Map<String, Object> quoteCart(Long userId, String couponCode) {
        CheckoutQuote quote = buildCartQuote(userId, couponCode);
        return quote.toPayload();
    }

    @Transactional
    public Map<String, Object> createOrderFromCart(Long userId, String couponCode) {
        CheckoutQuote quote = buildCartQuote(userId, couponCode);
        long orderId = orderRepository.createOrder(userId, quote.getPayableAmount());

        List<Long> purchasedSkuIds = new ArrayList<Long>();
        for (CatalogService.ResolvedOrderLine line : quote.getResolvedLines()) {
            BigDecimal lineAmount = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty().longValue()));
            orderRepository.addItem(orderId, line.getSkuId(), line.getQty(), line.getUnitPrice(), lineAmount);
            catalogService.consumeStockForOrder(line, Long.valueOf(orderId));
            purchasedSkuIds.add(line.getSkuId());
        }

        cartRepository.removeItems(userId, purchasedSkuIds);
        timelineRepository.addEvent(orderId, "order_submitted", "购物车结算下单成功，待支付");
        timelineRepository.addEvent(orderId,
                "checkout_pricing",
                String.format(Locale.ROOT,
                        "结算快照: 商品%.2f, 优惠%.2f, 预估税费%.2f, 应付%.2f",
                        quote.getGoodsAmount(),
                        quote.getCouponDiscount(),
                        quote.getTaxAmount(),
                        quote.getPayableAmount()));

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("orderStatus", OrderStatus.PENDING_PAYMENT.name());
        payload.put("payStatus", "UNPAID");
        payload.put("quote", quote.toPayload());
        return payload;
    }

    @Transactional
    public Map<String, Object> payOrder(Long orderId, String paymentChannel, String paymentScenario) {
        OrderMain order = getOrderOrThrow(orderId);
        if ("PAID".equals(order.getPayStatus())) {
            throw new ApiException("order already paid");
        }

        String scenario = paymentScenario == null ? "SUCCESS" : paymentScenario.trim().toUpperCase(Locale.ROOT);
        if ("FAIL".equals(scenario) || "FAIL_TIMEOUT".equals(scenario)) {
            orderRepository.markPayFailed(orderId);
            String token = buildCompensationToken(orderId);
            paymentCompensationRepository.createOpenRecord(orderId, paymentChannel, "PAYMENT_TIMEOUT", token);
            timelineRepository.addEvent(orderId, "payment_failed", "支付失败（超时），可发起补偿支付");

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("orderId", orderId);
            payload.put("orderStatus", OrderStatus.PENDING_PAYMENT.name());
            payload.put("payStatus", "FAILED");
            payload.put("paymentChannel", paymentChannel);
            payload.put("compensationToken", token);
            payload.put("failureReason", "PAYMENT_TIMEOUT");
            return payload;
        }

        return markPaidAndPublishTask(orderId, paymentChannel, "payment_success", "支付成功");
    }

    @Transactional
    public Map<String, Object> compensatePay(Long orderId, String paymentChannel, String compensationToken) {
        OrderMain order = getOrderOrThrow(orderId);
        if ("PAID".equals(order.getPayStatus())) {
            throw new ApiException("order already paid");
        }

        int affected = paymentCompensationRepository.consumeOpenToken(orderId, compensationToken);
        if (affected == 0) {
            throw new ApiException("compensation token invalid or already consumed");
        }

        return markPaidAndPublishTask(orderId, paymentChannel, "payment_compensated", "补偿支付成功");
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

    public long countPaymentFailedEvents() {
        return paymentCompensationRepository.countFailedPaymentEvents();
    }

    public long countPaymentCompensatedEvents() {
        return paymentCompensationRepository.countCompensatedPaymentEvents();
    }

    private Map<String, Object> markPaidAndPublishTask(Long orderId,
                                                       String paymentChannel,
                                                       String paymentEventType,
                                                       String paymentEventDescription) {
        orderRepository.markPaid(orderId);
        timelineRepository.addEvent(orderId, paymentEventType, paymentEventDescription + "，渠道: " + paymentChannel);

        TaskDispatchPlan dispatchPlan = buildTaskDispatchPlan(orderId);
        long taskId = taskRepository.createTask(
                orderId,
                new BigDecimal("20.00"),
                LocalDateTime.now().plusHours(dispatchPlan.getSlaHours().longValue()),
                dispatchPlan.getTaskTier(),
                dispatchPlan.getRequiredBuyerLevel(),
                dispatchPlan.getTargetRegion(),
                dispatchPlan.getTargetCategory(),
                dispatchPlan.getSlaHours()
        );
        timelineRepository.addEvent(orderId,
                "task_published",
                "采购任务已发布，分层:" + dispatchPlan.getTaskTier().name()
                        + "，最低买手等级:" + dispatchPlan.getRequiredBuyerLevel().name()
                        + "，目标品类:" + dispatchPlan.getTargetCategory());

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("orderId", orderId);
        payload.put("taskId", taskId);
        payload.put("orderStatus", OrderStatus.PAID_WAIT_ACCEPT.name());
        payload.put("payStatus", "PAID");
        payload.put("paymentChannel", paymentChannel);
        return payload;
    }

    private TaskDispatchPlan buildTaskDispatchPlan(Long orderId) {
        OrderMain order = getOrderOrThrow(orderId);
        BigDecimal totalAmount = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        TaskTier taskTier;
        BuyerLevel requiredLevel;
        int slaHours;
        if (totalAmount.compareTo(new BigDecimal("500.00")) >= 0) {
            taskTier = TaskTier.PRIORITY;
            requiredLevel = BuyerLevel.GOLD;
            slaHours = 24;
        } else if (totalAmount.compareTo(new BigDecimal("200.00")) >= 0) {
            taskTier = TaskTier.STANDARD;
            requiredLevel = BuyerLevel.SILVER;
            slaHours = 48;
        } else {
            taskTier = TaskTier.OPEN;
            requiredLevel = BuyerLevel.BRONZE;
            slaHours = 72;
        }

        String targetCategory = "GENERAL";
        List<OrderItem> items = orderRepository.findItems(orderId);
        if (!items.isEmpty()) {
            targetCategory = catalogService.resolveTaskCategoryBySku(items.get(0).getSkuId());
        }

        return new TaskDispatchPlan(taskTier, requiredLevel, "HK", targetCategory, Integer.valueOf(slaHours));
    }

    private CheckoutQuote buildCartQuote(Long userId, String couponCode) {
        List<CartItem> cartItems = cartRepository.listSelectedItems(userId);
        if (cartItems.isEmpty()) {
            throw new ApiException("cart is empty");
        }

        List<Map<String, Object>> itemPayloads = new ArrayList<Map<String, Object>>();
        List<CatalogService.ResolvedOrderLine> resolvedLines = new ArrayList<CatalogService.ResolvedOrderLine>();
        BigDecimal goodsAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            CatalogService.ResolvedOrderLine line = catalogService.resolveOrderLine(cartItem.getSkuId(), cartItem.getQty(), null);
            resolvedLines.add(line);

            BigDecimal lineAmount = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQty().longValue()));
            goodsAmount = goodsAmount.add(lineAmount);

            Map<String, Object> itemPayload = new LinkedHashMap<String, Object>();
            itemPayload.put("skuId", line.getSkuId());
            itemPayload.put("qty", line.getQty());
            itemPayload.put("unitPrice", line.getUnitPrice());
            itemPayload.put("lineAmount", lineAmount);
            itemPayloads.add(itemPayload);
        }

        CouponApplied couponApplied = resolveCoupon(couponCode, goodsAmount);
        BigDecimal taxableAmount = goodsAmount.subtract(couponApplied.getDiscountAmount());
        if (taxableAmount.signum() < 0) {
            taxableAmount = BigDecimal.ZERO;
        }

        BigDecimal taxAmount = taxableAmount.multiply(TAX_ESTIMATE_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        return new CheckoutQuote(userId,
                itemPayloads,
                resolvedLines,
                goodsAmount.setScale(2, RoundingMode.HALF_UP),
                couponApplied.getDiscountAmount(),
                taxAmount,
                payableAmount,
                couponApplied.getPayload());
    }

    private CouponApplied resolveCoupon(String couponCode, BigDecimal goodsAmount) {
        String normalized = couponCode == null ? "" : couponCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return CouponApplied.none();
        }

        Optional<CouponTemplate> optionalCoupon = couponRepository.findActiveCouponByCode(normalized, LocalDateTime.now());
        CouponTemplate coupon = optionalCoupon.orElseThrow(() -> new ApiException("coupon unavailable: " + normalized));

        BigDecimal minOrderAmount = coupon.getMinOrderAmount() == null ? BigDecimal.ZERO : coupon.getMinOrderAmount();
        if (goodsAmount.compareTo(minOrderAmount) < 0) {
            throw new ApiException("coupon minimum amount not met: " + minOrderAmount);
        }

        BigDecimal discountAmount;
        String discountType = coupon.getDiscountType() == null ? "FIXED" : coupon.getDiscountType().toUpperCase(Locale.ROOT);
        if ("FIXED".equals(discountType)) {
            discountAmount = coupon.getDiscountValue();
        } else if ("PERCENT".equals(discountType)) {
            discountAmount = goodsAmount.multiply(coupon.getDiscountValue());
        } else {
            throw new ApiException("unsupported coupon discount type: " + discountType);
        }

        if (discountAmount.compareTo(goodsAmount) > 0) {
            discountAmount = goodsAmount;
        }
        discountAmount = discountAmount.setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("couponCode", coupon.getCouponCode());
        payload.put("couponName", coupon.getCouponName());
        payload.put("discountType", discountType);
        payload.put("discountAmount", discountAmount);
        payload.put("minOrderAmount", minOrderAmount);

        return new CouponApplied(discountAmount, payload);
    }

    private String buildCompensationToken(Long orderId) {
        return "COMP-" + orderId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private static class CheckoutQuote {
        private final Long userId;
        private final List<Map<String, Object>> items;
        private final List<CatalogService.ResolvedOrderLine> resolvedLines;
        private final BigDecimal goodsAmount;
        private final BigDecimal couponDiscount;
        private final BigDecimal taxAmount;
        private final BigDecimal payableAmount;
        private final Map<String, Object> coupon;

        private CheckoutQuote(Long userId,
                              List<Map<String, Object>> items,
                              List<CatalogService.ResolvedOrderLine> resolvedLines,
                              BigDecimal goodsAmount,
                              BigDecimal couponDiscount,
                              BigDecimal taxAmount,
                              BigDecimal payableAmount,
                              Map<String, Object> coupon) {
            this.userId = userId;
            this.items = items;
            this.resolvedLines = resolvedLines;
            this.goodsAmount = goodsAmount;
            this.couponDiscount = couponDiscount;
            this.taxAmount = taxAmount;
            this.payableAmount = payableAmount;
            this.coupon = coupon;
        }

        public List<CatalogService.ResolvedOrderLine> getResolvedLines() {
            return resolvedLines;
        }

        public BigDecimal getGoodsAmount() {
            return goodsAmount;
        }

        public BigDecimal getCouponDiscount() {
            return couponDiscount;
        }

        public BigDecimal getTaxAmount() {
            return taxAmount;
        }

        public BigDecimal getPayableAmount() {
            return payableAmount;
        }

        public Map<String, Object> toPayload() {
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("userId", userId);
            payload.put("items", items);
            payload.put("goodsAmount", goodsAmount);
            payload.put("couponDiscount", couponDiscount);
            payload.put("taxAmount", taxAmount);
            payload.put("payableAmount", payableAmount);
            payload.put("coupon", coupon);
            payload.put("taxRate", TAX_ESTIMATE_RATE);
            return payload;
        }
    }

    private static class CouponApplied {
        private final BigDecimal discountAmount;
        private final Map<String, Object> payload;

        private CouponApplied(BigDecimal discountAmount, Map<String, Object> payload) {
            this.discountAmount = discountAmount;
            this.payload = payload;
        }

        public static CouponApplied none() {
            return new CouponApplied(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null);
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }
    }

    private static class TaskDispatchPlan {
        private final TaskTier taskTier;
        private final BuyerLevel requiredBuyerLevel;
        private final String targetRegion;
        private final String targetCategory;
        private final Integer slaHours;

        private TaskDispatchPlan(TaskTier taskTier,
                                 BuyerLevel requiredBuyerLevel,
                                 String targetRegion,
                                 String targetCategory,
                                 Integer slaHours) {
            this.taskTier = taskTier;
            this.requiredBuyerLevel = requiredBuyerLevel;
            this.targetRegion = targetRegion;
            this.targetCategory = targetCategory;
            this.slaHours = slaHours;
        }

        public TaskTier getTaskTier() {
            return taskTier;
        }

        public BuyerLevel getRequiredBuyerLevel() {
            return requiredBuyerLevel;
        }

        public String getTargetRegion() {
            return targetRegion;
        }

        public String getTargetCategory() {
            return targetCategory;
        }

        public Integer getSlaHours() {
            return slaHours;
        }
    }
}
