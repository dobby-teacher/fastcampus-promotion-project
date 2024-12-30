package com.fastcampus.couponservice.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {
    private final MeterRegistry registry;

    @Around("@annotation(Metered)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        
        try {
            Object result = joinPoint.proceed();
            
            // 성공 메트릭 기록
            Counter.builder("method.invocation.success")
                .tag("class", className)
                .tag("method", methodName)
                .register(registry)
                .increment();
                
            sample.stop(Timer.builder("method.execution.time")
                .tag("class", className)
                .tag("method", methodName)
                .register(registry));
                
            return result;
        } catch (Exception e) {
            // 실패 메트릭 기록
            Counter.builder("method.invocation.failure")
                .tag("class", className)
                .tag("method", methodName)
                .tag("exception", e.getClass().getSimpleName())
                .register(registry)
                .increment();
            throw e;
        }
    }
}