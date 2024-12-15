package com.fastcampus.couponservice.service.v3;

import com.fastcampus.couponservice.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponProducer {
    private static final String TOPIC = "coupon-issue-requests";
    private final KafkaTemplate<String, CouponDto.IssueMessage> kafkaTemplate;

    public void sendCouponIssueRequest(CouponDto.IssueMessage message) {
        kafkaTemplate.send(TOPIC, String.valueOf(message.getPolicyId()), message)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent message=[{}] with offset=[{}]", message, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send message=[{}] due to : {}", message, ex.getMessage());
                }
            });
    }
}
