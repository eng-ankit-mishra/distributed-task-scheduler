package com.ankitmishra.task_scheduler.service;

import com.ankitmishra.task_scheduler.domain.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j // 1. This automatically generates a 'log' field for you behind the scenes
public class JobExecutor {

    public void execute(Job job) {
        // 2. SLF4J maps parameters sequentially to each {} placeholder automatically
        log.info("Executing Job: [{}] - name: {} - payload: {}",
                job.getId(), job.getName(), job.getPayload());

        try {
            Thread.sleep(1000);
            log.info("Job [{}] completed Successfully", job.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 3. Passing 'e' as the last argument prints the full stack trace automatically
            log.error("Job [{}] interrupted", job.getId(), e);
            throw new RuntimeException("Job [" + job.getId() + "] interrupted", e);
        }
    }
}