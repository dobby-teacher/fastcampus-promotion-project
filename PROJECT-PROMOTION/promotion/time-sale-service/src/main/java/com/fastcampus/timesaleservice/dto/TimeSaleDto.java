package com.fastcampus.timesaleservice.dto;

import com.fastcampus.timesaleservice.domain.TimeSale;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

public class TimeSaleDto {

    @Getter
    @Builder
    public static class CreateRequest {
        @NotNull(message = "Product ID is required")
        private Long productId;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Long quantity;
        
        @NotNull(message = "Discount price is required")
        @Positive(message = "Discount price must be positive")
        private Long discountPrice;
        
        @NotNull(message = "Start time is required")
        @FutureOrPresent(message = "Start time must be current time or in the future")
        private LocalDateTime startAt;
        
        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        private LocalDateTime endAt;
    }

    @Getter
    @Builder
    public static class PurchaseRequest {
        @NotNull(message = "userId must not be null")
        private Long userId;
        
        @NotNull(message = "quantity must not be null")
        @Min(value = 1, message = "quantity must be greater than 0")
        private Long quantity;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long productId;
        private Long quantity;
        private Long remainingQuantity;
        private Long discountPrice;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private LocalDateTime createdAt;
        private String status;

        public static Response from(TimeSale timeSale) {
            return Response.builder()
                    .id(timeSale.getId())
                    .productId(timeSale.getProduct().getId())
                    .quantity(timeSale.getQuantity())
                    .remainingQuantity(timeSale.getRemainingQuantity())
                    .discountPrice(timeSale.getDiscountPrice())
                    .startAt(timeSale.getStartAt())
                    .endAt(timeSale.getEndAt())
                    .createdAt(timeSale.getCreatedAt())
                    .status(timeSale.getStatus().name())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PurchaseResponse {
        private Long timeSaleId;
        private Long userId;
        private Long productId;
        private Long quantity;
        private Long discountPrice;
        private LocalDateTime purchasedAt;
        private Long totalWaiting;    

        public static PurchaseResponse from(TimeSale timeSale, Long userId, Long quantity) {
            return PurchaseResponse.builder()
                    .timeSaleId(timeSale.getId())
                    .userId(userId)
                    .productId(timeSale.getProduct().getId())
                    .quantity(quantity)
                    .discountPrice(timeSale.getDiscountPrice())
                    .purchasedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AsyncPurchaseResponse {
        private String requestId;
        private String status;
        private Integer queuePosition;
        private Long totalWaiting;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseResponse2 {
        private String requestId;
        private String status;
    }
}
