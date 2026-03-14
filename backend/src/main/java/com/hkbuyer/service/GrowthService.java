package com.hkbuyer.service;

import com.hkbuyer.api.ApiException;
import com.hkbuyer.api.dto.CreateGrowthCampaignRequest;
import com.hkbuyer.domain.CatalogSku;
import com.hkbuyer.domain.GrowthCampaign;
import com.hkbuyer.domain.MembershipLevel;
import com.hkbuyer.domain.CouponTemplate;
import com.hkbuyer.repository.CatalogRepository;
import com.hkbuyer.repository.CouponRepository;
import com.hkbuyer.repository.GrowthRepository;
import com.hkbuyer.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GrowthService {

    private static final BigDecimal SILVER_AMOUNT_THRESHOLD = new BigDecimal("500.00");
    private static final BigDecimal GOLD_AMOUNT_THRESHOLD = new BigDecimal("2000.00");
    private static final long SILVER_ORDER_THRESHOLD = 2L;
    private static final long GOLD_ORDER_THRESHOLD = 5L;

    private final GrowthRepository growthRepository;
    private final CouponRepository couponRepository;
    private final OrderRepository orderRepository;
    private final CatalogRepository catalogRepository;
    private final CatalogService catalogService;

    public GrowthService(GrowthRepository growthRepository,
                         CouponRepository couponRepository,
                         OrderRepository orderRepository,
                         CatalogRepository catalogRepository,
                         CatalogService catalogService) {
        this.growthRepository = growthRepository;
        this.couponRepository = couponRepository;
        this.orderRepository = orderRepository;
        this.catalogRepository = catalogRepository;
        this.catalogService = catalogService;
    }

    @Transactional
    public Map<String, Object> getMembershipProfile(Long userId) {
        MembershipSnapshot snapshot = refreshMembershipSnapshot(userId);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", userId);
        payload.put("memberLevel", snapshot.membershipLevel.name());
        payload.put("totalPaidOrders", Long.valueOf(snapshot.totalPaidOrders));
        payload.put("totalPaidAmount", snapshot.totalPaidAmount);
        payload.put("memberPoints", Long.valueOf(snapshot.memberPoints));
        payload.put("benefits", resolveBenefits(snapshot.membershipLevel));
        payload.put("nextLevel", resolveNextLevel(snapshot));
        payload.put("refreshedAt", LocalDateTime.now());
        return payload;
    }

    public Map<String, Object> listUserCoupons(Long userId) {
        MembershipSnapshot snapshot = refreshMembershipSnapshot(userId);
        List<Map<String, Object>> allTouches = growthRepository.listUserTouches(userId, 100);
        List<Map<String, Object>> eligibleTouches = allTouches.stream()
                .filter(item -> isTouchEligibleForLevel(item, snapshot.membershipLevel))
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", userId);
        payload.put("memberLevel", snapshot.membershipLevel.name());
        payload.put("couponTouches", eligibleTouches);
        payload.put("total", Integer.valueOf(eligibleTouches.size()));
        return payload;
    }

    public Map<String, Object> getRecommendations(Long userId) {
        List<Long> repurchaseSkuIds = orderRepository.listTopPurchasedSkuIdsByUser(userId, 3);
        List<Map<String, Object>> repurchaseRecommendations = new ArrayList<Map<String, Object>>();
        for (Long skuId : repurchaseSkuIds) {
            try {
                repurchaseRecommendations.add(catalogService.getSkuDetail(skuId));
            } catch (ApiException ignored) {
                // Ignore unavailable sku, recommendation list should stay resilient.
            }
        }

        List<Map<String, Object>> categoryRecommendations = new ArrayList<Map<String, Object>>();
        if (!repurchaseSkuIds.isEmpty()) {
            Optional<String> category = catalogRepository.findCategoryBySkuId(repurchaseSkuIds.get(0));
            if (category.isPresent()) {
                List<CatalogSku> candidates = catalogRepository.listPublishedSkusByCategory(
                        category.get(),
                        repurchaseSkuIds,
                        6
                );
                for (CatalogSku candidate : candidates) {
                    categoryRecommendations.add(catalogService.getSkuDetail(candidate.getSkuId()));
                }
            }
        }

        if (categoryRecommendations.isEmpty()) {
            List<Map<String, Object>> fallback = catalogService.listCatalogSkus("", true);
            for (Map<String, Object> item : fallback) {
                Object skuIdValue = item.get("skuId");
                if (!(skuIdValue instanceof Number)) {
                    continue;
                }
                Long skuId = Long.valueOf(((Number) skuIdValue).longValue());
                if (repurchaseSkuIds.contains(skuId)) {
                    continue;
                }
                categoryRecommendations.add(item);
                if (categoryRecommendations.size() >= 6) {
                    break;
                }
            }
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userId", userId);
        payload.put("repurchaseRecommendations", repurchaseRecommendations);
        payload.put("categoryRecommendations", categoryRecommendations);
        payload.put("generatedAt", LocalDateTime.now());
        return payload;
    }

    @Transactional
    public Map<String, Object> createCampaign(CreateGrowthCampaignRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String couponCode = request.getCouponCode().trim().toUpperCase(Locale.ROOT);
        CouponTemplate coupon = couponRepository.findActiveCouponByCode(couponCode, now)
                .orElseThrow(() -> new ApiException("coupon unavailable for campaign: " + couponCode));

        String targetMemberLevel = normalizeTargetMemberLevel(request.getTargetMemberLevel());
        LocalDateTime startAt = parseDateTime(request.getStartAt(), "startAt");
        LocalDateTime endAt = parseDateTime(request.getEndAt(), "endAt");
        if (!endAt.isAfter(startAt)) {
            throw new ApiException("endAt must be later than startAt");
        }

        String campaignStatus;
        if (endAt.isBefore(now)) {
            campaignStatus = "EXPIRED";
        } else if (startAt.isAfter(now)) {
            campaignStatus = "SCHEDULED";
        } else {
            campaignStatus = "ACTIVE";
        }

        long campaignId = growthRepository.createCampaign(
                request.getCampaignName().trim(),
                targetMemberLevel,
                couponCode,
                campaignStatus,
                startAt,
                endAt,
                request.getAdminId()
        );

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("campaignId", Long.valueOf(campaignId));
        payload.put("campaignName", request.getCampaignName().trim());
        payload.put("targetMemberLevel", targetMemberLevel);
        payload.put("couponCode", coupon.getCouponCode());
        payload.put("campaignStatus", campaignStatus);
        payload.put("startAt", startAt);
        payload.put("endAt", endAt);
        payload.put("createdBy", request.getAdminId());
        return payload;
    }

    public List<Map<String, Object>> listCampaigns() {
        return growthRepository.listCampaigns(200).stream()
                .map(this::toCampaignPayload)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> publishCampaign(Long campaignId, Long adminId, List<Long> userIds, String touchChannel) {
        GrowthCampaign campaign = growthRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ApiException("campaign not found: " + campaignId));

        LocalDateTime now = LocalDateTime.now();
        if (campaign.getEndAt() != null && campaign.getEndAt().isBefore(now)) {
            growthRepository.updateCampaignStatus(campaignId, "EXPIRED");
            throw new ApiException("campaign already expired: " + campaignId);
        }

        if (campaign.getStartAt() != null && !campaign.getStartAt().isAfter(now)
                && !"ACTIVE".equals(campaign.getCampaignStatus())) {
            growthRepository.updateCampaignStatus(campaignId, "ACTIVE");
            campaign.setCampaignStatus("ACTIVE");
        }

        String normalizedTouchChannel = normalizeTouchChannel(touchChannel);

        Set<Long> deduplicatedUserIds = new LinkedHashSet<Long>();
        for (Long userId : userIds) {
            if (userId == null || userId.longValue() <= 0L) {
                continue;
            }
            deduplicatedUserIds.add(userId);
        }
        if (deduplicatedUserIds.isEmpty()) {
            throw new ApiException("userIds cannot be empty");
        }

        int touchedCount = 0;
        int skippedByTierCount = 0;
        for (Long userId : deduplicatedUserIds) {
            MembershipSnapshot snapshot = refreshMembershipSnapshot(userId);
            if (!isMemberLevelMatch(campaign.getTargetMemberLevel(), snapshot.membershipLevel)) {
                skippedByTierCount++;
                continue;
            }
            growthRepository.upsertTouch(
                    campaignId,
                    userId,
                    normalizedTouchChannel,
                    "SENT",
                    campaign.getCouponCode(),
                    now
            );
            touchedCount++;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("campaignId", campaignId);
        payload.put("campaignName", campaign.getCampaignName());
        payload.put("couponCode", campaign.getCouponCode());
        payload.put("campaignStatus", campaign.getCampaignStatus());
        payload.put("adminId", adminId);
        payload.put("touchChannel", normalizedTouchChannel);
        payload.put("requestedUsers", Integer.valueOf(userIds.size()));
        payload.put("deduplicatedUsers", Integer.valueOf(deduplicatedUserIds.size()));
        payload.put("touchedUsers", Integer.valueOf(touchedCount));
        payload.put("skippedByTierUsers", Integer.valueOf(skippedByTierCount));
        payload.put("publishedAt", now);
        return payload;
    }

    public Map<String, Object> buildGrowthMetrics() {
        long membershipTotal = growthRepository.countMembershipProfiles();
        long bronzeTotal = growthRepository.countMembershipProfilesByLevel(MembershipLevel.BRONZE);
        long silverTotal = growthRepository.countMembershipProfilesByLevel(MembershipLevel.SILVER);
        long goldTotal = growthRepository.countMembershipProfilesByLevel(MembershipLevel.GOLD);

        long campaignTotal = growthRepository.countCampaigns();
        long activeCampaignTotal = growthRepository.countActiveCampaigns(LocalDateTime.now());
        long touchSentTotal = growthRepository.countTouchesByStatus("SENT");
        long touchedUserTotal = growthRepository.countDistinctTouchedUsers();

        long couponUsedOrderTotal = growthRepository.countPaidOrdersWithCouponAttribution();
        long paidUser30dTotal = orderRepository.countPaidUsersWithinDays(30);
        long repurchaseUser30dTotal = orderRepository.countRepurchaseUsersWithinDays(30);

        double repurchaseRate30d = 0D;
        if (paidUser30dTotal > 0L) {
            repurchaseRate30d = (repurchaseUser30dTotal * 1.0D) / paidUser30dTotal;
        }

        double touchToCouponConversionRate = 0D;
        if (touchSentTotal > 0L) {
            touchToCouponConversionRate = (couponUsedOrderTotal * 1.0D) / touchSentTotal;
        }

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("membership_profile_total", Long.valueOf(membershipTotal));
        payload.put("membership_bronze_total", Long.valueOf(bronzeTotal));
        payload.put("membership_silver_total", Long.valueOf(silverTotal));
        payload.put("membership_gold_total", Long.valueOf(goldTotal));
        payload.put("growth_campaign_total", Long.valueOf(campaignTotal));
        payload.put("growth_campaign_active_total", Long.valueOf(activeCampaignTotal));
        payload.put("growth_touch_sent_total", Long.valueOf(touchSentTotal));
        payload.put("growth_touched_user_total", Long.valueOf(touchedUserTotal));
        payload.put("growth_coupon_used_order_total", Long.valueOf(couponUsedOrderTotal));
        payload.put("growth_touch_to_coupon_conversion_rate", Double.valueOf(touchToCouponConversionRate));
        payload.put("paid_user_30d_total", Long.valueOf(paidUser30dTotal));
        payload.put("repurchase_user_30d_total", Long.valueOf(repurchaseUser30dTotal));
        payload.put("repurchase_rate_30d", Double.valueOf(repurchaseRate30d));
        return payload;
    }

    public Optional<GrowthCampaign> findActiveCampaignByCouponCode(String couponCode, LocalDateTime now) {
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return Optional.empty();
        }
        return growthRepository.findActiveCampaignByCouponCode(couponCode.trim().toUpperCase(Locale.ROOT), now);
    }

    private MembershipSnapshot refreshMembershipSnapshot(Long userId) {
        long totalPaidOrders = orderRepository.countPaidOrdersByUser(userId);
        BigDecimal totalPaidAmount = normalizeMoney(orderRepository.sumPaidAmountByUser(userId));
        MembershipLevel membershipLevel = resolveMembershipLevel(totalPaidOrders, totalPaidAmount);

        long memberPoints = totalPaidAmount
                .setScale(0, RoundingMode.DOWN)
                .longValue();

        growthRepository.upsertMembershipProfile(
                userId,
                membershipLevel,
                totalPaidOrders,
                totalPaidAmount,
                memberPoints
        );

        return new MembershipSnapshot(totalPaidOrders, totalPaidAmount, membershipLevel, memberPoints);
    }

    private MembershipLevel resolveMembershipLevel(long totalPaidOrders, BigDecimal totalPaidAmount) {
        if (totalPaidOrders >= GOLD_ORDER_THRESHOLD || totalPaidAmount.compareTo(GOLD_AMOUNT_THRESHOLD) >= 0) {
            return MembershipLevel.GOLD;
        }
        if (totalPaidOrders >= SILVER_ORDER_THRESHOLD || totalPaidAmount.compareTo(SILVER_AMOUNT_THRESHOLD) >= 0) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.BRONZE;
    }

    private List<String> resolveBenefits(MembershipLevel level) {
        List<String> benefits = new ArrayList<String>();
        benefits.add("会员专属活动可见");
        if (level == MembershipLevel.SILVER || level == MembershipLevel.GOLD) {
            benefits.add("银卡专属券包");
        }
        if (level == MembershipLevel.GOLD) {
            benefits.add("金卡优先推荐与高价值券包");
        }
        return benefits;
    }

    private String resolveNextLevel(MembershipSnapshot snapshot) {
        if (snapshot.membershipLevel == MembershipLevel.GOLD) {
            return "MAX_LEVEL";
        }
        if (snapshot.membershipLevel == MembershipLevel.BRONZE) {
            long orderGap = Math.max(0L, SILVER_ORDER_THRESHOLD - snapshot.totalPaidOrders);
            BigDecimal amountGap = SILVER_AMOUNT_THRESHOLD.subtract(snapshot.totalPaidAmount);
            if (amountGap.signum() < 0) {
                amountGap = BigDecimal.ZERO;
            }
            return "升级 SILVER 还需支付单量 " + orderGap + " 单或金额 " + amountGap.setScale(2, RoundingMode.HALF_UP);
        }
        long orderGap = Math.max(0L, GOLD_ORDER_THRESHOLD - snapshot.totalPaidOrders);
        BigDecimal amountGap = GOLD_AMOUNT_THRESHOLD.subtract(snapshot.totalPaidAmount);
        if (amountGap.signum() < 0) {
            amountGap = BigDecimal.ZERO;
        }
        return "升级 GOLD 还需支付单量 " + orderGap + " 单或金额 " + amountGap.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isTouchEligibleForLevel(Map<String, Object> touch, MembershipLevel membershipLevel) {
        Object rawLevel = touch.get("targetMemberLevel");
        String targetMemberLevel = rawLevel == null ? "ALL" : rawLevel.toString();
        return isMemberLevelMatch(targetMemberLevel, membershipLevel);
    }

    private boolean isMemberLevelMatch(String targetMemberLevel, MembershipLevel membershipLevel) {
        if (targetMemberLevel == null || targetMemberLevel.trim().isEmpty()) {
            return true;
        }
        String normalized = targetMemberLevel.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) {
            return true;
        }
        return membershipLevel.name().equals(normalized);
    }

    private String normalizeTargetMemberLevel(String rawLevel) {
        String normalized = rawLevel.trim().toUpperCase(Locale.ROOT);
        if (!"BRONZE".equals(normalized)
                && !"SILVER".equals(normalized)
                && !"GOLD".equals(normalized)
                && !"ALL".equals(normalized)) {
            throw new ApiException("targetMemberLevel must be one of BRONZE/SILVER/GOLD/ALL");
        }
        return normalized;
    }

    private String normalizeTouchChannel(String touchChannel) {
        if (touchChannel == null || touchChannel.trim().isEmpty()) {
            return "IN_APP";
        }
        return touchChannel.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDateTime parseDateTime(String raw, String fieldName) {
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ex) {
            throw new ApiException(fieldName + " must use ISO format yyyy-MM-ddTHH:mm:ss");
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toCampaignPayload(GrowthCampaign campaign) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("campaignId", campaign.getCampaignId());
        payload.put("campaignName", campaign.getCampaignName());
        payload.put("targetMemberLevel", campaign.getTargetMemberLevel());
        payload.put("couponCode", campaign.getCouponCode());
        payload.put("campaignStatus", campaign.getCampaignStatus());
        payload.put("startAt", campaign.getStartAt());
        payload.put("endAt", campaign.getEndAt());
        payload.put("createdBy", campaign.getCreatedBy());
        payload.put("createdAt", campaign.getCreatedAt());
        payload.put("updatedAt", campaign.getUpdatedAt());
        return payload;
    }

    private static class MembershipSnapshot {
        private final long totalPaidOrders;
        private final BigDecimal totalPaidAmount;
        private final MembershipLevel membershipLevel;
        private final long memberPoints;

        private MembershipSnapshot(long totalPaidOrders,
                                   BigDecimal totalPaidAmount,
                                   MembershipLevel membershipLevel,
                                   long memberPoints) {
            this.totalPaidOrders = totalPaidOrders;
            this.totalPaidAmount = totalPaidAmount;
            this.membershipLevel = membershipLevel;
            this.memberPoints = memberPoints;
        }
    }
}
