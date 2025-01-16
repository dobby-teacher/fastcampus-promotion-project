package com.fastcampus.timesaleservice.dto;

import com.fastcampus.timesaleservice.domain.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ProductDto {

    @Getter
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        private Long price;

        @NotBlank(message = "Description is required")
        private String description;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String name;
        private Long price;
        private String description;
        private LocalDateTime createdAt;

        public static Response from(Product product) {
            return Response.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .description(product.getDescription())
                    .createdAt(product.getCreatedAt())
                    .build();
        }
    }
}
