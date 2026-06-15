package com.ankitmishra.task_scheduler.config;

import org.mockito.Mockito;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        RedissonClient mockClient = Mockito.mock(RedissonClient.class);
        RLock mockLock = Mockito.mock(RLock.class);

        // getLock() always returns the mock lock
        Mockito.when(mockClient.getLock(Mockito.anyString())).thenReturn(mockLock);

        // tryLock() returns true so the scheduler proceeds normally
        try {
            Mockito.when(mockLock.tryLock(
                    Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.any()
            )).thenReturn(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return mockClient;
    }
}
