package com.fastcampus.pointservicebatch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} is starting...", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job {} completed successfully", jobExecution.getJobInstance().getJobName());
        } else {
            log.error("Job {} failed with status {}", 
                jobExecution.getJobInstance().getJobName(), 
                jobExecution.getStatus());
        }
    }
}
