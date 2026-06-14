package com.ankitmishra.task_scheduler.messaging;

import com.ankitmishra.task_scheduler.config.RabbitMQConfig;
import com.ankitmishra.task_scheduler.domain.Job;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class JobPublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publish(Job job){
        JobMessage jobMessage = new JobMessage(
                job.getId(),
                job.getName(),
                job.getPayload(),
                job.getRetryCount()
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TASK_EXCHANGE,
                RabbitMQConfig.TASK_ROUTING_KEY,
                jobMessage
        );

        log.info("Published job [{}] to exchange '{}'", job.getId(), RabbitMQConfig.TASK_EXCHANGE);

    }




}
