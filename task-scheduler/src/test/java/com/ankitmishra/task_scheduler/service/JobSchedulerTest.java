package com.ankitmishra.task_scheduler.service;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.messaging.JobPublisher;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobSchedulerTest {

    @Mock private JobRepository jobRepository;
    @Mock
    private RedissonClient redissonClient;
    @Mock private JobPublisher jobPublisher;
    @Mock private RLock rLock;

    @InjectMocks private JobScheduler jobScheduler;

    @BeforeEach
    void setUp() {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
    }

    @Test
    void whenLockNotAcquired_shouldSkipPolling() throws InterruptedException {
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(false);

        jobScheduler.pollAndExecute();

        // Lock not acquired — DB must never be touched
        verify(jobRepository, never()).findDueJobs(any());
        verify(jobPublisher, never()).publish(any());
    }

    @Test
    void whenLockAcquired_shouldQueryForDueJobs() throws InterruptedException {
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(jobRepository.findDueJobs(any())).thenReturn(List.of());

        jobScheduler.pollAndExecute();

        verify(jobRepository, times(1)).findDueJobs(any());
        verify(rLock, times(1)).unlock();
    }

    @Test
    void whenExceptionThrown_lockMustStillBeReleased() throws InterruptedException {
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(jobRepository.findDueJobs(any())).thenThrow(new RuntimeException("DB is down"));

        // Exception should propagate but lock must release
        try { jobScheduler.pollAndExecute(); } catch (Exception ignored) {}

        verify(rLock, times(1)).unlock();
    }

    @Test
    void whenDueJobFound_shouldMarkRunningBeforePublishing() throws InterruptedException {
        Job job = buildJob(JobStatus.PENDING);
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        jobScheduler.pollAndExecute();

        // Verify save was called with RUNNING status
        verify(jobRepository, atLeastOnce()).save(argThat(j ->
                j.getStatus() == JobStatus.RUNNING
        ));
        verify(jobPublisher, times(1)).publish(any());
    }

    @Test
    void whenNoDueJobs_publisherNeverCalled() throws InterruptedException {
        when(rLock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(jobRepository.findDueJobs(any())).thenReturn(List.of());

        jobScheduler.pollAndExecute();

        verify(jobPublisher, never()).publish(any());
    }

    private Job buildJob(JobStatus status) {
        Job job = new Job();
        job.setName("test-job");
        job.setCronExpression("*/10 * * * * *");
        job.setStatus(status);
        job.setPayload("{\"action\":\"test\"}");
        job.setNextRunAt(LocalDateTime.now().minusSeconds(1));
        return job;
    }
}