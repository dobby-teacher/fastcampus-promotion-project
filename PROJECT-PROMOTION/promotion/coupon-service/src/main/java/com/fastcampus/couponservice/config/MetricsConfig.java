package com.fastcampus.couponservice.config;

import com.fastcampus.couponservice.aop.CouponMetricsAspect;
import com.fastcampus.couponservice.aop.MetricsAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {
    
    @Bean
    public MetricsAspect metricsAspect(MeterRegistry registry) {
        return new MetricsAspect(registry);
    }
    
    @Bean
    public CouponMetricsAspect couponMetricsAspect(MeterRegistry registry) {
        return new CouponMetricsAspect(registry);
    }
}