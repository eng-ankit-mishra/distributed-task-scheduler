package com.ankitmishra.task_scheduler.controller;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import com.ankitmishra.task_scheduler.util.CronExpressionParser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@AllArgsConstructor
public class JobController {
    private final JobRepository jobRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<List<Job>> getAllJobs() {
        return  ResponseEntity.ok(jobRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VIEWER')")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return  jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Job> createJob(@Valid @RequestBody CreateJobRequest request){
        if(!CronExpressionParser.isValid(request.cronExpression())){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Job job = new Job();
        job.setName(request.name());
        job.setCronExpression(request.cronExpression());
        job.setPayload(request.payload());
        job.setStatus(JobStatus.PENDING);
        job.setNextRunAt(CronExpressionParser.getNextRunTime(request.cronExpression()));

        return ResponseEntity.status(HttpStatus.CREATED).body(jobRepository.save(job));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Job> deleteJob(@PathVariable UUID id) {
        if(!jobRepository.existsById(id)){
            return ResponseEntity.notFound().build();
        }
        jobRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Job> triggerJob(@PathVariable UUID id){
        return jobRepository.findById(id).map(job->{
            job.setNextRunAt(LocalDateTime.now());
            job.setStatus(JobStatus.PENDING);
            jobRepository.save(job);
            return ResponseEntity.ok(job);
        }).orElse(ResponseEntity.notFound().build());
    }


    public record CreateJobRequest(@NotBlank String name,@NotBlank String cronExpression,String payload){
    }
    

}
