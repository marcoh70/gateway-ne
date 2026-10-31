package com.gateway.config;
import com.gateway.service.TokenExchangeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;

import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.logging.Logger;

@Slf4j
@Component
public class TokenExchangeGatewayFilterFactory
        extends AbstractGatewayFilterFactory<TokenExchangeGatewayFilterFactory.Config> {

    private final TokenExchangeService tokenExchangeService;

    public TokenExchangeGatewayFilterFactory(
            TokenExchangeService tokenExchangeService) {

        super(Config.class);
        this.tokenExchangeService = tokenExchangeService;
    }


    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            String authorization = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {

                exchange.getResponse().setStatusCode(
                        HttpStatus.UNAUTHORIZED);

                return exchange.getResponse().setComplete();
            }

            String originalToken =
                    authorization.substring("Bearer ".length());

            return tokenExchangeService
                    .exchange(originalToken, config.getTargetAudience())

                    .onErrorResume(error -> {

                        log.error("Token exchange failed for audience {}: {}",
                                config.getTargetAudience(),
                                error.getMessage());

                        exchange.getResponse().setStatusCode(
                                HttpStatus.BAD_GATEWAY);

                        return exchange.getResponse()
                                .setComplete()
                                .then(reactor.core.publisher.Mono.empty());
                    })

                    .flatMap(exchangedToken -> {

                        ServerWebExchange modifiedExchange =
                                exchange.mutate()
                                        .request(request ->
                                                request.headers(headers ->
                                                        headers.setBearerAuth(
                                                                exchangedToken)))
                                        .build();

                        return chain.filter(modifiedExchange);
                    });
        };
    }

    public static class Config {

        private String targetAudience;

        public String getTargetAudience() {
            return targetAudience;
        }

        public void setTargetAudience(String targetAudience) {
            this.targetAudience = targetAudience;
        }
    }

}


