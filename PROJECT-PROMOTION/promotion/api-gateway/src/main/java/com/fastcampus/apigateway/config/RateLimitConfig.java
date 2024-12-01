package com.fastcampus.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 초당 허용되는 요청 수
        // burstCapacity: 최대 누적 가능한 요청 수
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("X-User-ID") != null ?
            exchange.getRequest().getHeaders().getFirst("X-User-ID") :
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
}
