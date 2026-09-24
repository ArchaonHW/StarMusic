package com.starmusic.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }
}
