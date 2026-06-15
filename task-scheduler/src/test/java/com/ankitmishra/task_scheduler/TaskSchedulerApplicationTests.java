package com.ankitmishra.task_scheduler;

import com.ankitmishra.task_scheduler.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class TaskSchedulerApplicationTests {
	@Test
	void contextLoads() {
	}

}
