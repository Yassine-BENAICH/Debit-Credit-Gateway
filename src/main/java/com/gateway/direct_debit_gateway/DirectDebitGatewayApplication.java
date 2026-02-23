package com.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableRetry
@EnableAsync
@EnableScheduling
public class DebitCreditGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(DebitCreditGatewayApplication.class, args);
    }
}