package com.ankitmishra.task_scheduler.messaging;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobConsumerTest {

    @Mock private JobRepository jobRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private JobConsumer jobConsumer;

    @Test
    void successfulExecution_shouldSetStatusPendingAndResetRetryCount() {
        Job job = buildJob("*/10 * * * * *", 0);
        JobMessage message = buildMessage(job.getId(), 0);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        LocalDateTime before = LocalDateTime.now();

        jobConsumer.consume(message);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());

        Job saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(saved.getRetryCount()).isEqualTo(0);
        assertThat(saved.getNextRunAt()).isAfter(before);
    }

    @Test
    void firstFailure_shouldSetFailedStatusWithFiveSecondBackoff() {
        Job job = buildJob("*/10 * * * * *", 0);
        job.setPayload("fail");  // triggers simulated failure in consumer
        JobMessage message = buildMessage(job.getId(), 0);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        jobConsumer.consume(message);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());

        Job saved = captor.getAllValues().stream()
                .filter(j -> j.getStatus() == JobStatus.FAILED)
                .findFirst().orElseThrow();
        assertThat(saved.getRetryCount()).isEqualTo(1);
        assertThat(saved.getNextRunAt()).isAfter(LocalDateTime.now().plusSeconds(4));
    }

    @Test
    void secondFailure_shouldApplyTwentyFiveSecondBackoff() {
        Job job = buildJob("*/10 * * * * *", 1);
        job.setPayload("fail");
        JobMessage message = buildMessage(job.getId(), 1);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        jobConsumer.consume(message);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());

        Job saved = captor.getAllValues().stream()
                .filter(j -> j.getStatus() == JobStatus.FAILED)
                .findFirst().orElseThrow();
        assertThat(saved.getRetryCount()).isEqualTo(2);
        // 5^2 = 25 seconds backoff
        assertThat(saved.getNextRunAt()).isAfter(LocalDateTime.now().plusSeconds(24));
    }

    @Test
    void thirdFailure_shouldMarkDeadAndPublishToDlx() {
        Job job = buildJob("*/10 * * * * *", 2);
        job.setPayload("fail");
        JobMessage message = buildMessage(job.getId(), 2);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        when(jobRepository.save(any())).thenReturn(job);

        jobConsumer.consume(message);

        // Job must be marked DEAD
        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(j -> j.getStatus() == JobStatus.DEAD);

        // Message must be published to DLX
        verify(rabbitTemplate, times(1))
                .convertAndSend(anyString(), anyString(), any(JobMessage.class));
    }

    @Test
    void jobNotFoundInDb_shouldReturnGracefullyWithoutException() {
        UUID missingId = UUID.randomUUID();
        JobMessage message = buildMessage(missingId, 0);
        when(jobRepository.findById(missingId)).thenReturn(Optional.empty());

        // Must not throw — orphaned message should be silently discarded
        jobConsumer.consume(message);

        verify(jobRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(JobMessage.class));
    }

    private Job buildJob(String cron, int retryCount) {
        Job job = new Job();
        job.setName("test-job");
        job.setCronExpression(cron);
        job.setStatus(JobStatus.RUNNING);
        job.setRetryCount(retryCount);
        job.setNextRunAt(LocalDateTime.now());
        return job;
    }

    private JobMessage buildMessage(UUID jobId, int retryCount) {
        return new JobMessage(jobId, "test-job", "{}", retryCount);
    }
}