package com.fastcampus.timesaleservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TimeSaleServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TimeSaleServiceApplication.class, args);
    }
}
