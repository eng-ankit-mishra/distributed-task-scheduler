package com.ankitmishra.task_scheduler.service;

import com.ankitmishra.task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobSchedulerTest {
    @Mock private JobRepository jobRepository;
    @Mock private RedissonClient redissonClient;
    @Mock private JobExecutor jobExecutor;
    @Mock private RLock rLock;

    @InjectMocks private JobScheduler jobScheduler;


    @Test
    void whenLockNotAcquired_shouldSkipPolling() throws InterruptedException{
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(0,30, TimeUnit.SECONDS)).thenReturn(false);

        jobScheduler.pollAndExecute();

        verify(jobRepository,never()).findDueJobs(any());
    }

    @Test
    void whenLockAcquired_shouldPollForJobs() throws InterruptedException{
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(0,30,TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(jobRepository.findDueJobs(any())).thenReturn(java.util.List.of());

        jobScheduler.pollAndExecute();

        verify(jobRepository, times(1)).findDueJobs(any());
        verify(rLock,times(1)).unlock();

    }

    @Test
    void lockMustAlwaysBeReleasedEvenOnException() throws InterruptedException{
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(0,30,TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);

        try{jobScheduler.pollAndExecute();}catch (Exception e){}
        verify(rLock,times(1)).unlock();

    }

}
