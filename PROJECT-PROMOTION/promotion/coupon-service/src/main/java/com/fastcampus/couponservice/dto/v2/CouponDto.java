package com.fastcampus.couponservice.dto.v2;

import com.fastcampus.couponservice.domain.Coupon;
import com.fastcampus.couponservice.domain.CouponPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
public class CouponDto {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponIssueRequest {
        private Long couponPolicyId;
        private Long userId;
    }

    @Getter
    @Builder
    public static class CouponResponse {
        private Long id;
        private Long userId;
        private String couponCode;
        private CouponPolicy.DiscountType discountType;
        private int discountValue;
        private int minimumOrderAmount;
        private int maximumDiscountAmount;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private boolean used;
        private Long orderId;
        private LocalDateTime usedAt;

        public static CouponResponse from(Coupon coupon) {
            CouponPolicy policy = coupon.getCouponPolicy();
            return CouponResponse.builder()
                    .id(coupon.getId())
                    .userId(coupon.getUserId())
                    .couponCode(coupon.getCouponCode())
                    .discountType(policy.getDiscountType())
                    .discountValue(policy.getDiscountValue())
                    .minimumOrderAmount(policy.getMinimumOrderAmount())
                    .maximumDiscountAmount(policy.getMaximumDiscountAmount())
                    .validFrom(policy.getStartTime())
                    .validUntil(policy.getEndTime())
                    .used(coupon.isUsed())
                    .orderId(coupon.getOrderId())
                    .usedAt(coupon.getUsedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponUseRequest {
        private Long orderId;
        private Integer orderAmount;
    }
}
