package com.fastcampus.couponservice.service.v3;

import com.fastcampus.couponservice.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {
    private final CouponService couponService;

    @KafkaListener(topics = "coupon-issue-requests", groupId = "coupon-service", containerFactory = "couponKafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(CouponDto.IssueMessage message) {
        try {
            log.info("Received coupon issue request: {}", message);
            couponService.issueCoupon(message);
        } catch (Exception e) {
            log.error("Failed to process coupon issue request: {}", e.getMessage(), e);
        }
    }
}
