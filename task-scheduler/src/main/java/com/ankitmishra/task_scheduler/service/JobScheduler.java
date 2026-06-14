package com.ankitmishra.task_scheduler.service;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.messaging.JobPublisher;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import com.ankitmishra.task_scheduler.util.CronExpressionParser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@AllArgsConstructor
public class JobScheduler {

    private static final String SCHEDULER_LOCK="distributed-scheduler-lock";
    private static final int MAX_RETRIES=3;

    private final JobRepository jobRepository;
    private final RedissonClient redissonClient;
    private final JobExecutor jobExecutor;
    private final JobPublisher jobPublisher;

    @Scheduled(fixedDelay = 5000)
    public void pollAndExecute(){
        RLock lock=redissonClient.getLock(SCHEDULER_LOCK);

        boolean acquired=false;
        try{

            acquired=lock.tryLock(0,30, TimeUnit.SECONDS);

            if(!acquired){
                log.debug("Another instance is polling — skipping this cycle");
                return;
            }

            log.debug("Lock acquired - polling for due jobs");
            processDueJobs();

        }catch(InterruptedException e){

            Thread.currentThread().interrupt();
            log.error("Scheduler Interrupted while acquiring lock",e);

        }finally{
            if(acquired && lock.isHeldByCurrentThread()){
                lock.unlock();
                log.debug("Lock released");
            }
        }




    }
    @Transactional
    protected void processDueJobs(){
        List<Job> dueJobs=jobRepository.findDueJobs(LocalDateTime.now());

        if(dueJobs.isEmpty()){
            log.debug("No due jobs found");
            return;
        }

        log.info("Found {} job(s) - dispatching",dueJobs.size());

        for(Job job: dueJobs){
            dispatchJob(job);
        }
    }
    private void dispatchJob(Job job){
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        jobPublisher.publish(job);
        log.info("Job [{}] dispatched to queue", job.getId());

//        try{
//            jobExecutor.execute(job);
//
//            job.setStatus(JobStatus.SUCCESS);
//            job.setRetryCount(0);
//            job.setNextRunAt(CronExpressionParser.getNextRunTime(job.getCronExpression()));
//            job.setStatus(JobStatus.PENDING);
//            jobRepository.save(job);
//
//            log.info("Job [{}] succeeded — next run at {}", job.getId(), job.getNextRunAt());
//
//        }catch(Exception e){
//            handleFailure(job,e);
//        }
    }

    private void handleFailure(Job job,Exception e){
        int retries= job.getRetryCount() + 1;
        job.setRetryCount(retries);

        if(retries>=MAX_RETRIES){
            job.setStatus(JobStatus.DEAD);
            jobRepository.save(job);
            log.error("Job [{}] is DEAD after {} retries — moving to DLQ on Day 3",
                    job.getId(), retries, e);
        }else{
            long backOffSeconds=(long) Math.pow(5,retries);
            job.setStatus(JobStatus.FAILED);
            job.setNextRunAt(LocalDateTime.now().plusSeconds(backOffSeconds));
            jobRepository.save(job);
            log.warn("Job [{}] failed — retry {} scheduled in {}s",
                    job.getId(), retries, backOffSeconds, e);
        }
    }

}
