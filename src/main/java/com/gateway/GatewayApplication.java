package com.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {

		System.out.println(
				"API_GATEWAY_SECRET exists: " +
						(System.getenv("API_GATEWAY_SECRET") != null)
		);
		SpringApplication.run(GatewayApplication.class, args);
	}

	@RequestMapping(path = "/ente-fallback")
	public Mono<String> fallback() {
		return Mono.just("fallback");
	}

}
