package com.fastcampus.timesaleservice.config;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNode;
import org.redisson.api.redisnode.RedisNodes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    @Bean
    public MeterBinder redisMetrics(RedissonClient redissonClient) {
        return registry -> {
            Metrics.gauge("redis.memory_used_bytes", redissonClient, client -> {
                try {
                    Map<String, String> info = client.getRedisNodes(RedisNodes.SINGLE).getInstance().info(RedisNode.InfoSection.MEMORY);
                    return Double.parseDouble(info.get("used_memory"));
                } catch (Exception e) {
                    return 0.0;
                }
            });

            Metrics.gauge("redis.connected_clients", redissonClient, client -> {
                try {
                    Map<String, String> info = client.getRedisNodes(RedisNodes.SINGLE).getInstance().info(RedisNode.InfoSection.CLIENTS);
                    return Double.parseDouble(info.get("connected_clients"));
                } catch (Exception e) {
                    return 0.0;
                }
            });
        };
    }

    @Bean
    public KafkaClientMetrics kafkaConsumerMetrics(ConsumerFactory<String, ?> consumerFactory) {
        Consumer<String, ?> consumer = consumerFactory.createConsumer();
        return new KafkaClientMetrics(consumer);
    }

    @Bean
    public KafkaClientMetrics kafkaProducerMetrics(ProducerFactory<String, ?> producerFactory) {
        KafkaProducer<String, ?> producer = (KafkaProducer<String, ?>) producerFactory.createProducer();
        return new KafkaClientMetrics(producer);
    }
}