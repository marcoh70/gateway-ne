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

		SpringApplication app =
				new SpringApplication(GatewayApplication.class);

		app.addInitializers(context -> {
			var environment = context.getEnvironment();

			System.out.println(
					"ENV API_GATEWAY_SECRET: " +
							(environment.getProperty("API_GATEWAY_SECRET") )
			);

			System.out.println(
					"PROPERTY keycloak.client-secret: " +
							(environment.getProperty("keycloak.client-secret"))
			);
		});
		app.run(args);
	}

	@RequestMapping(path = "/ente-fallback")
	public Mono<String> fallback() {
		return Mono.just("fallback");
	}

}
