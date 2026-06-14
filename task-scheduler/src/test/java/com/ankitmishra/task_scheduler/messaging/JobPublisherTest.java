package com.ankitmishra.task_scheduler.messaging;

import com.ankitmishra.task_scheduler.config.RabbitMQConfig;
import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class JobPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @InjectMocks private JobPublisher jobPublisher;


    @Test
    void publish_shouldSendToCorrectExchangeAndRoutingKey(){
        Job job = new Job();

        jobPublisher.publish(job);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.TASK_EXCHANGE),
                eq(RabbitMQConfig.TASK_ROUTING_KEY),
                any(JobMessage.class)
        );
    }

    @Test
    void publish_shouldMapJobsFieldsToMessageCorrectly(){
        Job job = buildJob();

        jobPublisher.publish(job);

        ArgumentCaptor<JobMessage> captor = ArgumentCaptor.forClass(JobMessage.class);
        verify(rabbitTemplate).convertAndSend(anyString(),anyString(),captor.capture());

        JobMessage sent = captor.getValue();
        assertThat(sent.getJobId()).isEqualTo(job.getId());
        assertThat(sent.getJobName()).isEqualTo(job.getName());
        assertThat(sent.getPayload()).isEqualTo(job.getPayload());


    }

    private Job buildJob(){
        Job job=new Job();
        job.setName("email-job");
        job.setCronExpression("*/5 * * * * *");
        job.setPayload("{\"to\":\"user@example.com\"}");
        job.setStatus(JobStatus.PENDING);
        job.setNextRunAt(LocalDateTime.now());
        return job;
    }


}
