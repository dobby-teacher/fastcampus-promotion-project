package com.fastcampus.couponservice.dto.v2;

import com.fastcampus.couponservice.domain.CouponPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class CouponPolicyDto {

    @Getter
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "쿠폰 정책 이름은 필수입니다.")
        private String name;

        private String description;

        @NotNull(message = "할인 타입은 필수입니다.")
        private CouponPolicy.DiscountType discountType;

        @NotNull(message = "할인 값은 필수입니다.")
        @Min(value = 1, message = "할인 값은 1 이상이어야 합니다.")
        private Integer discountValue;

        @NotNull(message = "최소 주문 금액은 필수입니다.")
        @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.")
        private Integer minimumOrderAmount;

        @NotNull(message = "최대 할인 금액은 필수입니다.")
        @Min(value = 1, message = "최대 할인 금액은 1 이상이어야 합니다.")
        private Integer maximumDiscountAmount;

        @NotNull(message = "총 수량은 필수입니다.")
        @Min(value = 1, message = "총 수량은 1 이상이어야 합니다.")
        private Integer totalQuantity;

        @NotNull(message = "시작 시간은 필수입니다.")
        private LocalDateTime startTime;

        @NotNull(message = "종료 시간은 필수입니다.")
        private LocalDateTime endTime;

        public CouponPolicy toEntity() {
            return CouponPolicy.builder()
                    .name(name)
                    .description(description)
                    .discountType(discountType)
                    .discountValue(discountValue)
                    .minimumOrderAmount(minimumOrderAmount)
                    .maximumDiscountAmount(maximumDiscountAmount)
                    .totalQuantity(totalQuantity)
                    .startTime(startTime)
                    .endTime(endTime)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private CouponPolicy.DiscountType discountType;
        private Integer discountValue;
        private Integer minimumOrderAmount;
        private Integer maximumDiscountAmount;
        private Integer totalQuantity;
        private Integer issuedQuantity;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(CouponPolicy couponPolicy) {
            return Response.builder()
                    .id(couponPolicy.getId())
                    .name(couponPolicy.getName())
                    .description(couponPolicy.getDescription())
                    .discountType(couponPolicy.getDiscountType())
                    .discountValue(couponPolicy.getDiscountValue())
                    .minimumOrderAmount(couponPolicy.getMinimumOrderAmount())
                    .maximumDiscountAmount(couponPolicy.getMaximumDiscountAmount())
                    .totalQuantity(couponPolicy.getTotalQuantity())
                    .startTime(couponPolicy.getStartTime())
                    .endTime(couponPolicy.getEndTime())
                    .createdAt(couponPolicy.getCreatedAt())
                    .updatedAt(couponPolicy.getUpdatedAt())
                    .build();
        }
    }
}
