package com.fastcampus.springbatch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class JobLauncherController {
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;
    private final Job helloJob;
    private final Job customerFileJob;

    @GetMapping("/batch/hello")
    public ResponseEntity<String> runHelloJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
                    .addString("datetime", LocalDateTime.now().toString())
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(helloJob, jobParameters);
            
            return ResponseEntity.ok(
                "Job 실행 완료. 상태: " + jobExecution.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Job 실행 실패: " + e.getMessage());
        }
    }

    @GetMapping("/batch/customer-file")
    public ResponseEntity<String> runCustomerFileJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder(jobExplorer)
                    .addString("datetime", LocalDateTime.now().toString())
                    .toJobParameters();
            
            JobExecution jobExecution = 
                jobLauncher.run(customerFileJob, jobParameters);
            
            return ResponseEntity.ok(
                "Job 실행 완료. 상태: " + jobExecution.getStatus());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Job 실행 실패: " + e.getMessage());
        }
    }
}