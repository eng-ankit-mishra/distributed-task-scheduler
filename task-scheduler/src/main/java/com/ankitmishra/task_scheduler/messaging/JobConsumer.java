package com.ankitmishra.task_scheduler.messaging;

import com.ankitmishra.task_scheduler.config.RabbitMQConfig;
import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import com.ankitmishra.task_scheduler.util.CronExpressionParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;


@Service
@Slf4j
@AllArgsConstructor
public class JobConsumer {
    private static final int MAX_RETRIES=3;

    private  final JobRepository jobRepository;
    private  final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.TASK_QUEUE, concurrency = "3")
    public void consume(JobMessage message){
        UUID jobId=message.getJobId();
        log.info("Consuming Job [{}] - attempt {}",jobId,message.getRetryCount()+1);

        Job job=jobRepository.findById(jobId).orElse(null);

        if(job==null){
            log.warn("Job [{}] not found in DB - skipping", jobId);
            return;
        }

        try{
            executeJobLogic(job);
            handleSuccess(job);
        } catch (Exception e) {
            handleFailure(job,message,e);
        }
    }

    private void executeJobLogic(Job job) {
        // Simulate real work — replace with actual logic
        log.info("Executing business logic for job [{}]: {}", job.getId(), job.getPayload());
        if (job.getPayload() != null && job.getPayload().contains("fail")) {
            throw new RuntimeException("Simulated failure — payload contains 'fail'");
        }
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void handleSuccess(Job job) {
        job.setStatus(JobStatus.PENDING);
        job.setRetryCount(0);
        job.setNextRunAt(CronExpressionParser.getNextRunTime(job.getCronExpression()));
        jobRepository.save(job);
        log.info("Job [{}] succeeded — next run at {}", job.getId(), job.getNextRunAt());
    }

    private void handleFailure(Job job, JobMessage message, Exception e) {
        int retries = message.getRetryCount() + 1;
        log.warn("Job [{}] failed on attempt {} — reason: {}", job.getId(), retries, e.getMessage());

        if (retries >= MAX_RETRIES) {

            job.setStatus(JobStatus.DEAD);
            jobRepository.save(job);


            JobMessage deadMessage = new JobMessage(
                    job.getId(), job.getName(), job.getPayload(), retries
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.TASK_ROUTING_KEY,
                    deadMessage
            );
            log.error("Job [{}] is DEAD after {} retries — routed to DLQ", job.getId(), retries);

        } else {
            // Schedule retry with exponential backoff in DB
            long backoffSeconds = (long) Math.pow(5, retries);
            job.setStatus(JobStatus.FAILED);
            job.setRetryCount(retries);
            job.setNextRunAt(LocalDateTime.now().plusSeconds(backoffSeconds));
            jobRepository.save(job);
            log.warn("Job [{}] retry {} scheduled in {}s", job.getId(), retries, backoffSeconds);
        }
    }
}
