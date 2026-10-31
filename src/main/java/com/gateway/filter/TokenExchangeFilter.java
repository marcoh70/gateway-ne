package com.gateway.filter;

import com.gateway.service.TokenExchangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;



//@Component
public class TokenExchangeFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(TokenExchangeFilter.class);

    private static final String ENTE_PATH = "/api/ente";
    private static final String ENTE_AUDIENCE = "ente-service";

    private final TokenExchangeService tokenExchangeService;

    public TokenExchangeFilter(
            TokenExchangeService tokenExchangeService) {
        this.tokenExchangeService = tokenExchangeService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getRawPath();

        if (!path.equals(ENTE_PATH)
                && !path.startsWith(ENTE_PATH + "/")) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            exchange.getResponse().setStatusCode(
                    HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        String originalToken =
                authorization.substring("Bearer ".length());

        return tokenExchangeService
                .exchange(originalToken, ENTE_AUDIENCE)

                .onErrorResume(error -> {
                    log.error(
                            "Token exchange failed for route ente-route: {}",
                            error.getMessage());

                    exchange.getResponse().setStatusCode(
                            HttpStatus.BAD_GATEWAY);

                    return exchange.getResponse()
                            .setComplete()
                            .then(Mono.<String>empty());
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
    }

    @Override
    public int getOrder() {
        return -1;
    }
}