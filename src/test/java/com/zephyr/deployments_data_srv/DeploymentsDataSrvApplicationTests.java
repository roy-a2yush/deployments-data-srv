package com.zephyr.deployments_data_srv;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class DeploymentsDataSrvApplicationTests {

	@MockitoBean
	private KafkaAdmin kafkaAdmin;

	@Test
	void contextLoads() {
	}

}
