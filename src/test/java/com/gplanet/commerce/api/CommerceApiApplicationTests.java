package com.gplanet.commerce.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.gplanet.commerce.api.configs.TestRedisConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class CommerceApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
