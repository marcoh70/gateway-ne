package com.gateway.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;


@Service
public class TokenExchangeService {

    private final WebClient webClient;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    private final Environment environment;

    public TokenExchangeService(
            WebClient.Builder builder,
            @Value("${keycloak.token-url}") String tokenUrl,
            @Value("${keycloak.client-id}") String clientId, String clientId1, String clientSecret,
            Environment environment) {

        this.webClient = builder.build();
        this.tokenUrl = tokenUrl;
        this.clientId = clientId1;
        this.clientSecret = clientSecret;

        this.environment = environment;
    }

    public Mono<String> exchange(
            this.clientSecret = environment.getProperty("keycloak.client-secret");
            String subjectToken,
            String targetAudience) {

        if (!StringUtils.hasText(subjectToken)) {
            return Mono.error(
                    new TokenExchangeException("Subject token is empty"));
        }

        if (!StringUtils.hasText(targetAudience)) {
            return Mono.error(
                    new TokenExchangeException("Target audience is empty"));
        }

        var form = new LinkedMultiValueMap<String, String>();

        form.add("grant_type",
                "urn:ietf:params:oauth:grant-type:token-exchange");

        form.add("subject_token", subjectToken);

        form.add("subject_token_type",
                "urn:ietf:params:oauth:token-type:access_token");

        form.add("requested_token_type",
                "urn:ietf:params:oauth:token-type:access_token");

        form.add("audience", targetAudience);

        return webClient.post()
                .uri(tokenUrl)
                .headers(headers ->
                        headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(
                                        new TokenExchangeException(
                                                "Keycloak rejected token exchange. HTTP "
                                                        + response.statusCode().value()
                                        )
                                ))
                )
                .bodyToMono(TokenExchangeResponse.class)
                .timeout(Duration.ofSeconds(5))
                .switchIfEmpty(Mono.error(
                        new TokenExchangeException(
                                "Empty response from Keycloak")))
                .map(response -> {
                    if (!StringUtils.hasText(response.accessToken())) {
                        throw new TokenExchangeException(
                                "Keycloak response does not contain access_token");
                    }

                    return response.accessToken();
                });
    }

    private record TokenExchangeResponse(
            @JsonProperty("access_token") String accessToken) {
    }

    public static class TokenExchangeException
            extends RuntimeException {

        public TokenExchangeException(String message) {
            super(message);
        }
    }
}

