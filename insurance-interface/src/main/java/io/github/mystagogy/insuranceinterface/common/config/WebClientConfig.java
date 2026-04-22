package io.github.mystagogy.insuranceinterface.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
