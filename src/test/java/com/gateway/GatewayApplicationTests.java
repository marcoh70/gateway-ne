package com.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"keycloak.client-secret=test-secret"
})
class GatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
