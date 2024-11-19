package com.fastcampus.springbatch.config;

import com.fastcampus.springbatch.model.Customer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
@Slf4j
public class CustomerFileJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TaskExecutor taskExecutor;

    public CustomerFileJobConfig(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 @Qualifier("CustomerJobTaskExecutor") TaskExecutor taskExecutor) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.taskExecutor = taskExecutor;
    }

    @Bean
    public Job customerFileJob() {
        return new JobBuilder("customerFileJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(new JobLoggerListener())
                .start(customerFileStep())
                .build();
    }

    @Bean
    public Step customerFileStep() {
        return new StepBuilder("customerFileStep", jobRepository)
                .<Customer, Customer>chunk(10, transactionManager)
                .reader(customerFileReader())
                .processor(customerProcessor())
                .writer(customerWriter())
                .taskExecutor(taskExecutor)  // 멀티스레드 설정
                //.throttleLimit(5)            // 동시 실행 스레드 제한 (Deprecated)
                .listener(new ThreadMonitorListener(taskExecutor))
                .build();
    }

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<Customer> customerFileReader() {
        // Thread-safe를 위한 Synchronized Reader
        SynchronizedItemStreamReader<Customer> reader = new SynchronizedItemStreamReader<>();
        reader.setDelegate(
                new FlatFileItemReaderBuilder<Customer>()
                        .name("customerFileReader")
                        .resource(new ClassPathResource("customers.csv"))
                        .linesToSkip(1)
                        .delimited()
                        .names("id", "name", "email")
                        .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                            setTargetType(Customer.class);
                        }})
                        .build()
        );

        return reader;
    }

    @Bean
    public ItemProcessor<Customer, Customer> customerProcessor() {
        return customer -> {
            customer.setRegisteredDate(LocalDateTime.now());
            return customer;
        };
    }

    @Bean
    public ItemWriter<Customer> customerWriter() {
        return items -> {
            for (Customer customer : items) {
                log.info("Customer 저장: {}", customer);
            }
        };
    }
}