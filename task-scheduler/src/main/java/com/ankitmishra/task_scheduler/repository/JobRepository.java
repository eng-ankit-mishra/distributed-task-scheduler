package com.ankitmishra.task_scheduler.repository;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    @Query("SELECT j FROM Job j WHERE j.status IN ('PENDING', 'FAILED') AND j.nextRunAt <= :now")
    List<Job> findDueJobs(LocalDateTime now);
}
