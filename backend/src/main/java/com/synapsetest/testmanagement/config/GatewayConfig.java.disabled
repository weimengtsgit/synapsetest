package com.synapsetest.testmanagement.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway Configuration (Simplified for MVP)
 * Routes requests to appropriate services
 */
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("ai_service_route", r -> r
                        .path("/api/v1/ai/**")
                        .uri("http://localhost:8000"))
                .route("backend_route", r -> r
                        .path("/api/v1/**")
                        .uri("http://localhost:8080"))
                .build();
    }
}
