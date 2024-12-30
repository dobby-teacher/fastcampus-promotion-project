package com.fastcampus.pointservicebatch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing
@SpringBootApplication
public class PointServiceBatchApplication {

    private final JobLauncher jobLauncher;
    private final Job pointBalanceSyncJob;

    public PointServiceBatchApplication(JobLauncher jobLauncher, Job pointBalanceSyncJob) {
        this.jobLauncher = jobLauncher;
        this.pointBalanceSyncJob = pointBalanceSyncJob;
    }

    public static void main(String[] args) {
        SpringApplication.run(PointServiceBatchApplication.class, args);
    }

    @Bean
    public ApplicationRunner runner() {
        return args -> {
            jobLauncher.run(
                    pointBalanceSyncJob,
                    new JobParametersBuilder()
                            .addLong("timestamp", System.currentTimeMillis()) // JobParameters 예시
                            .toJobParameters()
            );
        };
    }
}
