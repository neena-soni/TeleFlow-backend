package com.teleflow.user_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${teleflow.services.orchestrator.base-url:http://localhost:8081}")
    private String orchestratorBaseUrl;

    @Bean
    public WebClient orchestratorWebClient() {
        return WebClient.builder()
                .baseUrl(orchestratorBaseUrl)
                .build();
    }
}
