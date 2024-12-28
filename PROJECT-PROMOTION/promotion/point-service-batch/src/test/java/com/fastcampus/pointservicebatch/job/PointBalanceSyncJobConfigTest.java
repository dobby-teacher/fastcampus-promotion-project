package com.fastcampus.pointservicebatch.job;

import com.fastcampus.pointservicebatch.domain.Point;
import com.fastcampus.pointservicebatch.domain.PointBalance;
import com.fastcampus.pointservicebatch.domain.PointType;
import com.fastcampus.pointservicebatch.domain.DailyPointReport;
import com.fastcampus.pointservicebatch.repository.PointBalanceRepository;
import com.fastcampus.pointservicebatch.repository.PointRepository;
import com.fastcampus.pointservicebatch.repository.DailyPointReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.batch.job.enabled=false"
})
class PointBalanceSyncJobConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private PointRepository pointRepository;

    @MockBean
    private PointBalanceRepository pointBalanceRepository;

    @Autowired
    private DailyPointReportRepository dailyPointReportRepository;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private RMap<String, Long> balanceMap;

    @BeforeEach
    void setUp() {
        // Redis mock 설정
        when(redissonClient.<String, Long>getMap(anyString())).thenReturn(balanceMap);

        // 테스트 데이터 초기화
        dailyPointReportRepository.deleteAll();
        
        // 테스트 데이터 생성
        createTestData();
    }

    @Test
    @DisplayName("포인트 동기화 Job 실행 성공 테스트")
    void jobExecutionTest() throws Exception {
        // given
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        
        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
    }

    @Test
    @DisplayName("Redis 캐시 동기화 Step 테스트")
    void syncPointBalanceStepTest() throws Exception {
        // given
        PointBalance pointBalance = PointBalance.builder()
                .userId(1L)
                .balance(1000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(pointBalanceRepository.findByUserId(1L)).thenReturn(java.util.Optional.of(pointBalance));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("syncPointBalanceStep", jobParameters);
        
        // then
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("일별 리포트 생성 Step 테스트")
    void generateDailyReportStepTest() throws Exception {
        // given
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Point> points = Arrays.asList(
            Point.builder()
                .userId(1L)
                .amount(1000L)
                .type(PointType.EARN)
                .balanceSnapshot(1000L)
                .createdAt(yesterday)
                .build()
        );
        when(pointRepository.findAllByCreatedAtBetween(
            yesterday.withHour(0).withMinute(0).withSecond(0),
            yesterday.withHour(23).withMinute(59).withSecond(59)
        )).thenReturn(points);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("datetime", LocalDateTime.now().toString())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("generateDailyReportStep", jobParameters);
        
        // then
        assertThat(jobExecution.getStepExecutions()).hasSize(1);
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    private void createTestData() {
        // 테스트 데이터 설정
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        List<Point> points = Arrays.asList(
            Point.builder()
                .userId(1L)
                .amount(1000L)
                .type(PointType.EARN)
                .balanceSnapshot(1000L)
                .createdAt(yesterday)
                .build()
        );
        when(pointRepository.findAllByCreatedAtBetween(
            yesterday.withHour(0).withMinute(0).withSecond(0),
            yesterday.withHour(23).withMinute(59).withSecond(59)
        )).thenReturn(points);
    }
}
