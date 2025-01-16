package com.fastcampus.timesaleservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestMessage {
    private Long timeSaleId;
    private Long userId;
    private Long quantity;
    private String requestId;
}
