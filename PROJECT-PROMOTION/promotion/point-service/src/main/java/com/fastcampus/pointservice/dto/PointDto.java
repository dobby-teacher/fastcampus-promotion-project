package com.fastcampus.pointservice.dto;

import com.fastcampus.pointservice.domain.Point;
import com.fastcampus.pointservice.domain.PointType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class PointDto {

    @Getter
    @Builder
    public static class EarnRequest {
        private Long userId;
        
        @NotNull(message = "amount must not be null")
        @Min(value = 1, message = "amount must be greater than 0")
        private Long amount;
        
        @NotBlank(message = "description must not be blank")
        private String description;
    }

    @Getter
    @Builder
    public static class UseRequest {
        private Long userId;

        @NotNull(message = "amount must not be null")
        @Min(value = 1, message = "amount must be greater than 0")
        private Long amount;
        
        @NotBlank(message = "description must not be blank")
        private String description;
    }

    @Getter
    @Builder
    public static class CancelRequest {
        @NotNull(message = "pointId must not be null")
        private Long pointId;
        
        @NotBlank(message = "description must not be blank")
        private String description;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private Long amount;
        private PointType type;
        private String description;
        private Long balanceSnapshot;
        private LocalDateTime createdAt;

        public static Response from(Point point) {
            return Response.builder()
                    .id(point.getId())
                    .userId(point.getUserId())
                    .amount(point.getAmount())
                    .type(point.getType())
                    .description(point.getDescription())
                    .balanceSnapshot(point.getBalanceSnapshot())
                    .createdAt(point.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class BalanceResponse {
        private Long userId;
        private Long balance;

        public static BalanceResponse of(Long userId, Long balance) {
            return BalanceResponse.builder()
                    .userId(userId)
                    .balance(balance)
                    .build();
        }
    }
}
