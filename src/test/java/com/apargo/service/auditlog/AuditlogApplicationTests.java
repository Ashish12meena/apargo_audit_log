package com.apargo.service.auditlog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"dev", "kafka"})
class AuditlogApplicationTests {

	@Test
	void contextLoads() {
	}

}
