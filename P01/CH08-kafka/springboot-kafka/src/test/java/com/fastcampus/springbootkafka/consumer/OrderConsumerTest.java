package com.fastcampus.springbootkafka.consumer;

import com.fastcampus.springbootkafka.model.OrderEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@SpringBootTest
class OrderConsumerTest {

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @SpyBean
    private OrderConsumer consumer;

    @Test
    void testOrderProcessing() {
        // Given
        OrderEvent order = createTestOrder();

        // When
        kafkaTemplate.send("orders", order.getOrderId(), order);

        // Then
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(consumer, times(1)).processOrder(order)
        );
    }

    private OrderEvent createTestOrder() {
        List<OrderEvent.OrderItem> items = List.of(new OrderEvent.OrderItem("prod-1", 2, BigDecimal.valueOf(20.00)));
        return new OrderEvent("order-123", "cust-456", items, BigDecimal.valueOf(40.00), LocalDateTime.now());
    }
}
