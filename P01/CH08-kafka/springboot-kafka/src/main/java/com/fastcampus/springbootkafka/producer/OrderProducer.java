package com.fastcampus.springbootkafka.producer;

import com.fastcampus.springbootkafka.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "orders";

    public void sendOrder(OrderEvent order) {
        kafkaTemplate.send(TOPIC, order.getOrderId(), order)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message: {}", order.getOrderId(), ex);
                    } else {
                        log.info("Message sent successfully: {}, partition: {}",
                                order.getOrderId(), result.getRecordMetadata().partition());
                    }
                });
    }

    public void sendOrderSync(OrderEvent order) throws Exception {
        try {
            SendResult<String, OrderEvent> result = kafkaTemplate.send(TOPIC, order.getOrderId(), order).get();
            log.info("Message sent synchronously: {}, partition: {}", 
                order.getOrderId(), result.getRecordMetadata().partition());
        } catch (Exception e) {
            log.error("Error sending message synchronously", e);
            throw e;
        }
    }
}