package com.fastcampus.springbatch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class ThreadMonitorListener implements StepExecutionListener {
    private final ThreadPoolTaskExecutor taskExecutor;

    public ThreadMonitorListener(TaskExecutor taskExecutor) {
        this.taskExecutor = (ThreadPoolTaskExecutor) taskExecutor;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Active threads: {}, Pool size: {}", 
            taskExecutor.getActiveCount(),
            taskExecutor.getPoolSize());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("Final thread stats - Active: {}, Pool size: {}, Completed tasks: {}", 
            taskExecutor.getActiveCount(),
            taskExecutor.getPoolSize(),
            taskExecutor.getThreadPoolExecutor().getCompletedTaskCount());
        return stepExecution.getExitStatus();
    }
}