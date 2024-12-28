package com.fastcampus.pointservicebatch.job;

import com.fastcampus.pointservicebatch.domain.DailyPointReport;
import com.fastcampus.pointservicebatch.domain.Point;
import com.fastcampus.pointservicebatch.domain.PointBalance;
import com.fastcampus.pointservicebatch.domain.PointSummary;
import com.fastcampus.pointservicebatch.listener.JobCompletionNotificationListener;
import com.fastcampus.pointservicebatch.repository.DailyPointReportRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;

/**
 * 포인트 잔액 동기화 및 일별 리포트 생성을 위한 배치 Job 설정
 * 
 * 주요 기능:
 * 1. Redis 캐시와 DB의 포인트 잔액 동기화
 * 2. 전일 포인트 트랜잭션 기반 일별 리포트 생성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PointBalanceSyncJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RedissonClient redissonClient;
    private final DailyPointReportRepository dailyPointReportRepository;
    private final JobCompletionNotificationListener jobCompletionNotificationListener;

    /**
     * 포인트 잔액 동기화 및 일별 리포트 생성 Job
     * 
     * 실행 순서:
     * 1. syncPointBalanceStep: DB의 포인트 잔액을 Redis 캐시에 동기화
     * 2. generateDailyReportStep: 전일 포인트 트랜잭션을 집계하여 일별 리포트 생성
     */
    @Bean
    public Job pointBalanceSyncJob() {
        return new JobBuilder("pointBalanceSyncJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(syncPointBalanceStep())
                .next(generateDailyReportStep())
                .build();
    }

    /**
     * 포인트 잔액 동기화 Step
     * 
     * DB의 포인트 잔액 정보를 Redis 캐시에 동기화하는 Step
     * - Reader: JPA를 통해 포인트 잔액 조회
     * - Processor: 캐시 키 생성
     * - Writer: Redis에 포인트 잔액 저장
     */
    @Bean
    public Step syncPointBalanceStep() {
        return new StepBuilder("syncPointBalanceStep", jobRepository)
                .<PointBalance, Map.Entry<String, Long>>chunk(1000, transactionManager)
                .reader(pointBalanceReader())
                .processor(pointBalanceProcessor())
                .writer(pointBalanceWriter())
                .build();
    }

    /**
     * 일별 리포트 생성 Step
     * 
     * 전일 포인트 트랜잭션을 집계하여 일별 리포트를 생성하는 Step
     * - Reader: JPA를 통해 전일 포인트 트랜잭션 조회
     * - Processor: 포인트 트랜잭션을 사용자별로 집계
     * - Writer: 일별 리포트를 DB에 저장
     */
    @Bean
    public Step generateDailyReportStep() {
        return new StepBuilder("generateDailyReportStep", jobRepository)
                .<Point, PointSummary>chunk(1000, transactionManager)
                .reader(pointReader())
                .processor(pointProcessor())
                .writer(reportWriter())
                .build();
    }

    /**
     * 포인트 잔액 Reader
     * 
     * JPA를 사용하여 DB에서 포인트 잔액 정보를 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<PointBalance> pointBalanceReader() {
        return new JpaPagingItemReaderBuilder<PointBalance>()
                .name("pointBalanceReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT pb FROM PointBalance pb")
                .build();
    }

    /**
     * 포인트 잔액 Processor
     * 
     * 포인트 잔액을 Redis 캐시 키-값 쌍으로 변환
     */
    @Bean
    @StepScope
    public ItemProcessor<PointBalance, Map.Entry<String, Long>> pointBalanceProcessor() {
        return pointBalance -> Map.entry(
                String.format("point:balance:%d", pointBalance.getUserId()),
                pointBalance.getBalance()
        );
    }

    /**
     * 포인트 잔액 Writer
     * 
     * Redis 캐시에 포인트 잔액 저장
     */
    @Bean
    @StepScope
    public ItemWriter<Map.Entry<String, Long>> pointBalanceWriter() {
        return items -> {
            var balanceMap = redissonClient.getMap("point:balance");
            items.forEach(item -> balanceMap.put(item.getKey(), item.getValue()));
        };
    }

    /**
     * 포인트 트랜잭션 Reader
     * 
     * 전일의 포인트 트랜잭션을 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Point> pointReader() {
        Map<String, Object> parameters = new HashMap<>();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        parameters.put("startTime", yesterday.withHour(0).withMinute(0).withSecond(0));
        parameters.put("endTime", yesterday.withHour(23).withMinute(59).withSecond(59));

        return new JpaPagingItemReaderBuilder<Point>()
                .name("pointReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(1000)
                .queryString("SELECT p FROM Point p WHERE p.createdAt BETWEEN :startTime AND :endTime")
                .parameterValues(parameters)
                .build();
    }

    /**
     * 포인트 트랜잭션 Processor
     * 
     * 포인트 트랜잭션을 사용자별로 집계하여 PointSummary 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<Point, PointSummary> pointProcessor() {
        return point -> {
            switch (point.getType()) {
                case EARN -> {
                    return new PointSummary(point.getUserId(), point.getAmount(), 0L, 0L);
                }
                case USE -> {
                    return new PointSummary(point.getUserId(), 0L, point.getAmount(), 0L);
                }
                case CANCEL -> {
                    return new PointSummary(point.getUserId(), 0L, 0L, point.getAmount());
                }
                default -> {
                    return null;
                }
            }
        };
    }

    /**
     * 일별 리포트 Writer
     * 
     * 집계된 포인트 트랜잭션을 일별 리포트로 변환하여 DB에 저장
     */
    @Bean
    @StepScope
    public ItemWriter<PointSummary> reportWriter() {
        return summaries -> {
            List<DailyPointReport> reports = new ArrayList<>();
            for (PointSummary summary : summaries) {
                DailyPointReport report = DailyPointReport.builder()
                        .userId(summary.getUserId())
                        .reportDate(now().minusDays(1))  // 전일 데이터
                        .earnAmount(summary.getEarnAmount())
                        .useAmount(summary.getUseAmount())
                        .cancelAmount(summary.getCancelAmount())
                        .build();
                reports.add(report);
            }

            dailyPointReportRepository.saveAll(reports);
        };
    }
}
