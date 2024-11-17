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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class CustomerFileJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

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
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Customer> customerFileReader() {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(new ClassPathResource("customers.csv"))
                .delimited()
                .names("id", "name", "email")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(Customer.class);
                }})
                .build();
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