package com.capgemini.authservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires running PostgreSQL and config-server — run only in integration environment")
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
