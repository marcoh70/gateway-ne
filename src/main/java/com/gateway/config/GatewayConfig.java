package com.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {

        return builder.routes()
                .route("ente-route", r -> r
                        .path("/api/ente/**")
                        .filters(f -> f
                                .retry(retryConfig -> retryConfig
                                        .setRetries(3)
                                        .setStatuses(HttpStatus.BAD_GATEWAY,
                                                HttpStatus.SERVICE_UNAVAILABLE,
                                                HttpStatus.GATEWAY_TIMEOUT)
                                        .setMethods(HttpMethod.GET)
                                )
                                .circuitBreaker(config -> config
                                        .setName("enteCircuitBreaker")
                                        .setFallbackUri("forward:/ente-fallback")
                                )
                        )
                        .uri("http://localhost:9091")
                )
                .build();
    }
}
